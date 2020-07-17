/*
 * Copyright (C) 2004-2020 L2J Server
 *
 * This file is part of L2J Server.
 *
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.l2jserver.Config;
import com.l2jserver.util.StringUtil;

/**
 * <p>
 * This class is made to handle all the ThreadPools used in L2J.
 * </p>
 * <p>
 * Scheduled Tasks can either be sent to a {@link #_generalScheduledThreadPool "general"} or {@link #_effectsScheduledThreadPool "effects"} {@link ScheduledThreadPoolExecutor ScheduledThreadPool}: The "effects" one is used for every effects (skills, hp/mp regen ...) while the "general" one is used
 * for everything else that needs to be scheduled.<br>
 * There also is an {@link #_aiScheduledThreadPool "ai"} {@link ScheduledThreadPoolExecutor ScheduledThreadPool} used for AI Tasks.
 * </p>
 * <p>
 * Tasks can be sent to {@link ScheduledThreadPoolExecutor ScheduledThreadPool} either with:
 * <ul>
 * <li>{@link #scheduleEffect(Runnable, long, TimeUnit)} and {@link #scheduleEffect(Runnable, long)} : for effects Tasks that needs to be executed only once.</li>
 * <li>{@link #scheduleGeneral(Runnable, long, TimeUnit)} and {@link #scheduleGeneral(Runnable, long)} : for scheduled Tasks that needs to be executed once.</li>
 * <li>{@link #scheduleAi(Runnable, long, TimeUnit)} and {@link #scheduleAi(Runnable, long)} : for AI Tasks that needs to be executed once</li>
 * </ul>
 * or
 * <ul>
 * <li>{@link #scheduleEffectAtFixedRate(Runnable, long, long, TimeUnit)} and {@link #scheduleEffectAtFixedRate(Runnable, long, long)} : for effects Tasks that needs to be executed periodically.</li>
 * <li>{@link #scheduleGeneralAtFixedRate(Runnable, long, long, TimeUnit)} and {@link #scheduleGeneralAtFixedRate(Runnable, long, long)} : for scheduled Tasks that needs to be executed periodically.</li>
 * <li>{@link #scheduleAiAtFixedRate(Runnable, long, long, TimeUnit)} and {@link #scheduleAiAtFixedRate(Runnable, long, long)} : for AI Tasks that needs to be executed periodically</li>
 * </ul>
 * </p>
 * <p>
 * For all Tasks that should be executed with no delay asynchronously in a ThreadPool there also are usual {@link ThreadPoolExecutor ThreadPools} that can grow/shrink according to their load.:
 * <ul>
 * <li>{@link #_generalPacketsThreadPool GeneralPackets} where most packets handler are executed.</li>
 * <li>{@link #_ioPacketsThreadPool I/O Packets} where all the i/o packets are executed.</li>
 * <li>There will be an AI ThreadPool where AI events should be executed</li>
 * <li>A general ThreadPool where everything else that needs to run asynchronously with no delay should be executed ({@link com.l2jserver.gameserver.model.actor.knownlist KnownList} updates, SQL updates/inserts...)?</li>
 * </ul>
 * </p>
 * @author -Wooden-, Sacrifice
 */
public final class ThreadPoolManager
{
	private static final Logger LOG = LoggerFactory.getLogger(ThreadPoolManager.class);
	
	private final ScheduledThreadPoolExecutor _aiScheduledThreadPool;
	private final ScheduledThreadPoolExecutor _effectsScheduledThreadPool;
	private final ScheduledThreadPoolExecutor _eventsScheduledThreadPool;
	private final ScheduledThreadPoolExecutor _generalScheduledThreadPool;
	
	private final ThreadPoolExecutor _eventsThreadPool;
	private final ThreadPoolExecutor _generalPacketsThreadPool;
	private final ThreadPoolExecutor _generalThreadPool;
	private final ThreadPoolExecutor _ioPacketsThreadPool;
	
	private boolean _shutdown;
	
	private ThreadPoolManager()
	{
		_aiScheduledThreadPool = new ScheduledThreadPoolExecutor(Config.SCHEDULED_THREAD_CORE_POOL_SIZE_AI <= 0 ? Runtime.getRuntime().availableProcessors() : Config.SCHEDULED_THREAD_CORE_POOL_SIZE_AI, new PriorityThreadFactory("AI ST Pool", Thread.NORM_PRIORITY));
		_aiScheduledThreadPool.setRemoveOnCancelPolicy(true); // Since Java7, Explicitly call setRemoveOnCancelPolicy on the instance. This prevents from memory leaks when using ScheduledExecutorService
		_effectsScheduledThreadPool = new ScheduledThreadPoolExecutor(Config.SCHEDULED_THREAD_CORE_POOL_SIZE_EFFECTS <= 0 ? Runtime.getRuntime().availableProcessors() : Config.SCHEDULED_THREAD_CORE_POOL_SIZE_EFFECTS, new PriorityThreadFactory("Effects ST Pool", Thread.NORM_PRIORITY));
		_effectsScheduledThreadPool.setRemoveOnCancelPolicy(true); // Since Java7, Explicitly call setRemoveOnCancelPolicy on the instance. This prevents from memory leaks when using ScheduledExecutorService
		_eventsScheduledThreadPool = new ScheduledThreadPoolExecutor(Config.SCHEDULED_THREAD_CORE_POOL_SIZE_EVENTS <= 0 ? Runtime.getRuntime().availableProcessors() : Config.SCHEDULED_THREAD_CORE_POOL_SIZE_EVENTS, new PriorityThreadFactory("Event ST Pool", Thread.NORM_PRIORITY));
		_eventsScheduledThreadPool.setRemoveOnCancelPolicy(true); // Since Java7, Explicitly call setRemoveOnCancelPolicy on the instance. This prevents from memory leaks when using ScheduledExecutorService
		_generalScheduledThreadPool = new ScheduledThreadPoolExecutor(Config.SCHEDULED_THREAD_CORE_POOL_SIZE_GENERAL <= 0 ? Runtime.getRuntime().availableProcessors() : Config.SCHEDULED_THREAD_CORE_POOL_SIZE_GENERAL, new PriorityThreadFactory("General ST Pool", Thread.NORM_PRIORITY));
		_generalScheduledThreadPool.setRemoveOnCancelPolicy(true); // Since Java7, Explicitly call setRemoveOnCancelPolicy on the instance. This prevents from memory leaks when using ScheduledExecutorService
		
		//@formatter:off
		_eventsThreadPool = new ThreadPoolExecutor(Config.THREAD_CORE_POOL_SIZE_EVENT <= 0 ? Runtime.getRuntime().availableProcessors() : Config.THREAD_CORE_POOL_SIZE_EVENT, Config.THREAD_CORE_POOL_SIZE_EVENT <= 0 ? Runtime.getRuntime().availableProcessors() : Config.THREAD_CORE_POOL_SIZE_EVENT, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new PriorityThreadFactory("Event Pool", Thread.NORM_PRIORITY));
		_generalPacketsThreadPool = new ThreadPoolExecutor(Config.THREAD_CORE_POOL_SIZE_GENERAL_PACKETS <= 0 ? Runtime.getRuntime().availableProcessors() : Config.THREAD_CORE_POOL_SIZE_GENERAL_PACKETS, Config.THREAD_CORE_POOL_SIZE_GENERAL_PACKETS <= 0 ? Runtime.getRuntime().availableProcessors() : Config.THREAD_CORE_POOL_SIZE_GENERAL_PACKETS, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new PriorityThreadFactory("Normal Packet Pool", Thread.NORM_PRIORITY + 1));
		_generalThreadPool = new ThreadPoolExecutor(Config.THREAD_CORE_POOL_SIZE_GENERAL <= 0 ? Runtime.getRuntime().availableProcessors() : Config.THREAD_CORE_POOL_SIZE_GENERAL, Config.THREAD_CORE_POOL_SIZE_GENERAL <= 0 ? Runtime.getRuntime().availableProcessors() : Config.THREAD_CORE_POOL_SIZE_GENERAL, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new PriorityThreadFactory("General Pool", Thread.NORM_PRIORITY));
		_ioPacketsThreadPool = new ThreadPoolExecutor(Config.THREAD_CORE_POOL_SIZE_IO_PACKETS <= 0 ? Runtime.getRuntime().availableProcessors() : Config.THREAD_CORE_POOL_SIZE_IO_PACKETS, Integer.MAX_VALUE, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new PriorityThreadFactory("I/O Packet Pool", Thread.NORM_PRIORITY + 1));
		//@formatter:on
		
		scheduleGeneralAtFixedRate(new PurgeTask(_aiScheduledThreadPool, _effectsScheduledThreadPool, _eventsScheduledThreadPool, _generalScheduledThreadPool, //
			_eventsThreadPool, _generalPacketsThreadPool, _generalThreadPool, _ioPacketsThreadPool), 10, 5, TimeUnit.MINUTES);
	}
	
	@SuppressWarnings("synthetic-access")
	public static ThreadPoolManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	/**
	 * Executes an AI task sometime in future in another thread.
	 * @param task the task to execute
	 */
	public void executeAi(Runnable task)
	{
		try
		{
			_aiScheduledThreadPool.execute(new RunnableWrapper(task));
		}
		catch (RejectedExecutionException e)
		{
			/* shutdown, ignore */
		}
	}
	
	/**
	 * Executes an Event task sometime in future in another thread.
	 * @param task the task to execute
	 */
	public void executeEvent(Runnable task)
	{
		try
		{
			_eventsThreadPool.execute(new RunnableWrapper(task));
		}
		catch (RejectedExecutionException e)
		{
			/* shutdown, ignore */
		}
	}
	
	/**
	 * Executes a general task sometime in future in another thread.
	 * @param task the task to execute
	 */
	public void executeGeneral(Runnable task)
	{
		try
		{
			_generalThreadPool.execute(new RunnableWrapper(task));
		}
		catch (RejectedExecutionException e)
		{
			/* shutdown, ignore */
		}
	}
	
	/**
	 * Executes an IO packet task sometime in future in another thread.
	 * @param task the task to execute
	 */
	public void executeIOPacket(Runnable task)
	{
		try
		{
			_ioPacketsThreadPool.execute(task);
		}
		catch (RejectedExecutionException e)
		{
			/* shutdown, ignore */
		}
	}
	
	/**
	 * Executes a packet task sometime in future in another thread.
	 * @param task the task to execute
	 */
	public void executePacket(Runnable task)
	{
		try
		{
			_generalPacketsThreadPool.execute(task);
		}
		catch (RejectedExecutionException e)
		{
			/* shutdown, ignore */
		}
	}
	
	public String getGeneralStats()
	{
		final StringBuilder sb = new StringBuilder(1000);
		final ThreadFactory tf = _generalThreadPool.getThreadFactory();
		if (tf instanceof PriorityThreadFactory)
		{
			final PriorityThreadFactory ptf = (PriorityThreadFactory) tf;
			final int count = ptf.getGroup().activeCount();
			final Thread[] threads = new Thread[count + 2];
			ptf.getGroup().enumerate(threads);
			StringUtil.append(sb, "General Thread Pool:" + Config.EOL + "Tasks in the queue: ", String.valueOf(_generalThreadPool.getQueue().size()), Config.EOL + "Showing threads stack trace:" + Config.EOL + "There should be ", String.valueOf(count), " Threads" + Config.EOL);
			for (Thread t : threads)
			{
				if (t == null)
				{
					continue;
				}
				StringUtil.append(sb, t.getName(), Config.EOL);
				for (StackTraceElement ste : t.getStackTrace())
				{
					StringUtil.append(sb, ste.toString(), Config.EOL);
				}
			}
		}
		sb.append("Packet Tp stack traces printed.");
		sb.append(Config.EOL);
		return sb.toString();
	}
	
	public String getIOPacketStats()
	{
		final StringBuilder sb = new StringBuilder(1000);
		final ThreadFactory tf = _ioPacketsThreadPool.getThreadFactory();
		if (tf instanceof PriorityThreadFactory)
		{
			final PriorityThreadFactory ptf = (PriorityThreadFactory) tf;
			final int count = ptf.getGroup().activeCount();
			final Thread[] threads = new Thread[count + 2];
			ptf.getGroup().enumerate(threads);
			StringUtil.append(sb, "I/O Packet Thread Pool:" + Config.EOL + "Tasks in the queue: ", String.valueOf(_ioPacketsThreadPool.getQueue().size()), Config.EOL + "Showing threads stack trace:" + Config.EOL + "There should be ", String.valueOf(count), " Threads" + Config.EOL);
			for (Thread t : threads)
			{
				if (t == null)
				{
					continue;
				}
				StringUtil.append(sb, t.getName(), Config.EOL);
				for (StackTraceElement ste : t.getStackTrace())
				{
					StringUtil.append(sb, ste.toString(), Config.EOL);
				}
			}
		}
		sb.append("Packet Tp stack traces printed.");
		sb.append(Config.EOL);
		return sb.toString();
	}
	
	public String getPacketStats()
	{
		final StringBuilder sb = new StringBuilder(1000);
		final ThreadFactory tf = _generalPacketsThreadPool.getThreadFactory();
		if (tf instanceof PriorityThreadFactory)
		{
			final PriorityThreadFactory ptf = (PriorityThreadFactory) tf;
			final int count = ptf.getGroup().activeCount();
			final Thread[] threads = new Thread[count + 2];
			ptf.getGroup().enumerate(threads);
			StringUtil.append(sb, "General Packet Thread Pool:" + Config.EOL + "Tasks in the queue: ", String.valueOf(_generalPacketsThreadPool.getQueue().size()), Config.EOL + "Showing threads stack trace:" + Config.EOL + "There should be ", String.valueOf(count), " Threads" + Config.EOL);
			for (Thread t : threads)
			{
				if (t == null)
				{
					continue;
				}
				StringUtil.append(sb, t.getName(), Config.EOL);
				for (StackTraceElement ste : t.getStackTrace())
				{
					StringUtil.append(sb, ste.toString(), Config.EOL);
				}
			}
		}
		sb.append("Packet Tp stack traces printed.");
		sb.append(Config.EOL);
		return sb.toString();
	}
	
	/**
	 * Gets the thread pools stats.
	 * @return the stats
	 */
	public List<String> getStats()
	{
		final List<String> list = new ArrayList<>();
		list.add("<<:: Scheduled Pool ::>>");
		list.add("==================================");
		list.add("Effects:");
		list.add("ActiveThreads:   " + _effectsScheduledThreadPool.getActiveCount());
		list.add("CorePoolSize:    " + _effectsScheduledThreadPool.getCorePoolSize());
		list.add("PoolSize:        " + _effectsScheduledThreadPool.getPoolSize());
		list.add("MaximumPoolSize: " + _effectsScheduledThreadPool.getMaximumPoolSize());
		list.add("CompletedTasks:  " + _effectsScheduledThreadPool.getCompletedTaskCount());
		list.add("ScheduledTasks:  " + _effectsScheduledThreadPool.getQueue().size());
		list.add("==================================");
		list.add("General:");
		list.add("ActiveThreads:   " + _generalScheduledThreadPool.getActiveCount());
		list.add("CorePoolSize:    " + _generalScheduledThreadPool.getCorePoolSize());
		list.add("PoolSize:        " + _generalScheduledThreadPool.getPoolSize());
		list.add("MaximumPoolSize: " + _generalScheduledThreadPool.getMaximumPoolSize());
		list.add("CompletedTasks:  " + _generalScheduledThreadPool.getCompletedTaskCount());
		list.add("ScheduledTasks:  " + _generalScheduledThreadPool.getQueue().size());
		list.add("==================================");
		list.add("AI:");
		list.add("ActiveThreads:   " + _aiScheduledThreadPool.getActiveCount());
		list.add("CorePoolSize:    " + _aiScheduledThreadPool.getCorePoolSize());
		list.add("PoolSize:        " + _aiScheduledThreadPool.getPoolSize());
		list.add("MaximumPoolSize: " + _aiScheduledThreadPool.getMaximumPoolSize());
		list.add("CompletedTasks:  " + _aiScheduledThreadPool.getCompletedTaskCount());
		list.add("ScheduledTasks:  " + _aiScheduledThreadPool.getQueue().size());
		list.add("==================================");
		list.add("Event:");
		list.add("ActiveThreads:   " + _eventsScheduledThreadPool.getActiveCount());
		list.add("CorePoolSize:    " + _eventsScheduledThreadPool.getCorePoolSize());
		list.add("PoolSize:        " + _eventsScheduledThreadPool.getPoolSize());
		list.add("MaximumPoolSize: " + _eventsScheduledThreadPool.getMaximumPoolSize());
		list.add("CompletedTasks:  " + _eventsScheduledThreadPool.getCompletedTaskCount());
		list.add("ScheduledTasks:  " + _eventsScheduledThreadPool.getQueue().size());
		list.add("==================================");
		list.add("<<:: Thread Pool ::>>");
		list.add("Packets:");
		list.add("ActiveThreads:   " + _generalPacketsThreadPool.getActiveCount());
		list.add("CorePoolSize:    " + _generalPacketsThreadPool.getCorePoolSize());
		list.add("MaximumPoolSize: " + _generalPacketsThreadPool.getMaximumPoolSize());
		list.add("LargestPoolSize: " + _generalPacketsThreadPool.getLargestPoolSize());
		list.add("PoolSize:        " + _generalPacketsThreadPool.getPoolSize());
		list.add("CompletedTasks:  " + _generalPacketsThreadPool.getCompletedTaskCount());
		list.add("QueuedTasks:     " + _generalPacketsThreadPool.getQueue().size());
		list.add("==================================");
		list.add("I/O Packets:");
		list.add("ActiveThreads:   " + _ioPacketsThreadPool.getActiveCount());
		list.add("CorePoolSize:    " + _ioPacketsThreadPool.getCorePoolSize());
		list.add("MaximumPoolSize: " + _ioPacketsThreadPool.getMaximumPoolSize());
		list.add("LargestPoolSize: " + _ioPacketsThreadPool.getLargestPoolSize());
		list.add("PoolSize:        " + _ioPacketsThreadPool.getPoolSize());
		list.add("CompletedTasks:  " + _ioPacketsThreadPool.getCompletedTaskCount());
		list.add("QueuedTasks:     " + _ioPacketsThreadPool.getQueue().size());
		list.add("==================================");
		list.add("General Tasks:");
		list.add("ActiveThreads:   " + _generalThreadPool.getActiveCount());
		list.add("CorePoolSize:    " + _generalThreadPool.getCorePoolSize());
		list.add("MaximumPoolSize: " + _generalThreadPool.getMaximumPoolSize());
		list.add("LargestPoolSize: " + _generalThreadPool.getLargestPoolSize());
		list.add("PoolSize:        " + _generalThreadPool.getPoolSize());
		list.add("CompletedTasks:  " + _generalThreadPool.getCompletedTaskCount());
		list.add("QueuedTasks:     " + _generalThreadPool.getQueue().size());
		list.add("==================================");
		list.add("Event Tasks:");
		list.add("ActiveThreads:   " + _eventsThreadPool.getActiveCount());
		list.add("CorePoolSize:    " + _eventsThreadPool.getCorePoolSize());
		list.add("MaximumPoolSize: " + _eventsThreadPool.getMaximumPoolSize());
		list.add("LargestPoolSize: " + _eventsThreadPool.getLargestPoolSize());
		list.add("PoolSize:        " + _eventsThreadPool.getPoolSize());
		list.add("CompletedTasks:  " + _eventsThreadPool.getCompletedTaskCount());
		list.add("QueuedTasks:     " + _eventsThreadPool.getQueue().size());
		list.add("==================================");
		return list;
	}
	
	public boolean isShutdown()
	{
		return _shutdown;
	}
	
	public void purge()
	{
		_aiScheduledThreadPool.purge();
		_effectsScheduledThreadPool.purge();
		_eventsScheduledThreadPool.purge();
		_generalScheduledThreadPool.purge();
		
		_eventsThreadPool.purge();
		_generalPacketsThreadPool.purge();
		_generalThreadPool.purge();
		_ioPacketsThreadPool.purge();
	}
	
	/**
	 * Schedules an AI task to be executed after the given delay.
	 * @param task the task to execute
	 * @param delay the delay in milliseconds
	 * @return a ScheduledFuture representing pending completion of the task, and whose get() method will throw an exception upon cancellation
	 */
	public ScheduledFuture<?> scheduleAi(Runnable task, long delay)
	{
		return scheduleAi(task, delay, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Schedules an AI task to be executed after the given delay.
	 * @param task the task to execute
	 * @param delay the delay in the given time unit
	 * @param unit the time unit of the delay parameter
	 * @return a ScheduledFuture representing pending completion of the task, and whose get() method will throw an exception upon cancellation
	 */
	public ScheduledFuture<?> scheduleAi(Runnable task, long delay, TimeUnit unit)
	{
		try
		{
			return _aiScheduledThreadPool.schedule(new RunnableWrapper(task), delay, unit);
		}
		catch (RejectedExecutionException e)
		{
			return null; /* shutdown, ignore */
		}
	}
	
	/**
	 * Schedules an AI task to be executed at fixed rate.
	 * @param task the task to execute
	 * @param initialDelay the initial delay in milliseconds
	 * @param period the period between executions in milliseconds
	 * @return a ScheduledFuture representing pending completion of the task, and whose get() method will throw an exception upon cancellation
	 */
	public ScheduledFuture<?> scheduleAiAtFixedRate(Runnable task, long initialDelay, long period)
	{
		return scheduleAiAtFixedRate(task, initialDelay, period, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Schedules an AI task to be executed at fixed rate.
	 * @param task the task to execute
	 * @param initialDelay the initial delay in the given time unit
	 * @param period the period between executions in the given time unit
	 * @param unit the time unit of the initialDelay and period parameters
	 * @return a ScheduledFuture representing pending completion of the task, and whose get() method will throw an exception upon cancellation
	 */
	public ScheduledFuture<?> scheduleAiAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit)
	{
		try
		{
			return _aiScheduledThreadPool.scheduleAtFixedRate(new RunnableWrapper(task), initialDelay, period, unit);
		}
		catch (RejectedExecutionException e)
		{
			return null; /* shutdown, ignore */
		}
	}
	
	/**
	 * Schedules an effect task to be executed after the given delay.
	 * @param task the task to execute
	 * @param delay the delay in milliseconds
	 * @return a ScheduledFuture representing pending completion of the task, and whose get() method will throw an exception upon cancellation
	 */
	public ScheduledFuture<?> scheduleEffect(Runnable task, long delay)
	{
		return scheduleEffect(task, delay, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Schedules an effect task to be executed after the given delay.
	 * @param task the task to execute
	 * @param delay the delay in the given time unit
	 * @param unit the time unit of the delay parameter
	 * @return a ScheduledFuture representing pending completion of the task, and whose get() method will throw an exception upon cancellation
	 */
	public ScheduledFuture<?> scheduleEffect(Runnable task, long delay, TimeUnit unit)
	{
		try
		{
			return _effectsScheduledThreadPool.schedule(new RunnableWrapper(task), delay, unit);
		}
		catch (RejectedExecutionException e)
		{
			return null; /* shutdown, ignore */
		}
	}
	
	/**
	 * Schedules an effect task to be executed at fixed rate.
	 * @param task the task to execute
	 * @param initialDelay the initial delay in milliseconds
	 * @param period the period between executions in milliseconds
	 * @return a ScheduledFuture representing pending completion of the task, and whose get() method will throw an exception upon cancellation
	 */
	public ScheduledFuture<?> scheduleEffectAtFixedRate(Runnable task, long initialDelay, long period)
	{
		return scheduleEffectAtFixedRate(task, initialDelay, period, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Schedules an effect task to be executed at fixed rate.
	 * @param task the task to execute
	 * @param initialDelay the initial delay in the given time unit
	 * @param period the period between executions in the given time unit
	 * @param unit the time unit of the initialDelay and period parameters
	 * @return a ScheduledFuture representing pending completion of the task, and whose get() method will throw an exception upon cancellation
	 */
	public ScheduledFuture<?> scheduleEffectAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit)
	{
		try
		{
			return _effectsScheduledThreadPool.scheduleAtFixedRate(new RunnableWrapper(task), initialDelay, period, unit);
		}
		catch (RejectedExecutionException e)
		{
			return null; /* shutdown, ignore */
		}
	}
	
	/**
	 * Schedules a event task to be executed after the given delay.
	 * @param task the task to execute
	 * @param delay the delay in milliseconds
	 * @return a ScheduledFuture representing pending completion of the task, and whose get() method will throw an exception upon cancellation
	 */
	public ScheduledFuture<?> scheduleEvent(Runnable task, long delay)
	{
		return scheduleEvent(task, delay, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Schedules a event task to be executed after the given delay.
	 * @param task the task to execute
	 * @param delay the delay in the given time unit
	 * @param unit the time unit of the delay parameter
	 * @return a ScheduledFuture representing pending completion of the task, and whose get() method will throw an exception upon cancellation
	 */
	public ScheduledFuture<?> scheduleEvent(Runnable task, long delay, TimeUnit unit)
	{
		try
		{
			return _eventsScheduledThreadPool.schedule(new RunnableWrapper(task), delay, unit);
		}
		catch (RejectedExecutionException e)
		{
			return null; /* shutdown, ignore */
		}
	}
	
	/**
	 * Schedules a event task to be executed at fixed rate.
	 * @param task the task to execute
	 * @param initialDelay the initial delay in the given time unit
	 * @param period the period between executions in the given time unit
	 * @param unit the time unit of the initialDelay and period parameters
	 * @return a ScheduledFuture representing pending completion of the task, and whose get() method will throw an exception upon cancellation
	 */
	public ScheduledFuture<?> scheduleEventAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit)
	{
		try
		{
			return _eventsScheduledThreadPool.scheduleAtFixedRate(new RunnableWrapper(task), initialDelay, period, unit);
		}
		catch (RejectedExecutionException e)
		{
			return null; /* shutdown, ignore */
		}
	}
	
	/**
	 * Schedules a general task to be executed after the given delay.
	 * @param task the task to execute
	 * @param delay the delay in milliseconds
	 * @return a ScheduledFuture representing pending completion of the task, and whose get() method will throw an exception upon cancellation
	 */
	public ScheduledFuture<?> scheduleGeneral(Runnable task, long delay)
	{
		return scheduleGeneral(task, delay, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Schedules a general task to be executed after the given delay.
	 * @param task the task to execute
	 * @param delay the delay in the given time unit
	 * @param unit the time unit of the delay parameter
	 * @return a ScheduledFuture representing pending completion of the task, and whose get() method will throw an exception upon cancellation
	 */
	public ScheduledFuture<?> scheduleGeneral(Runnable task, long delay, TimeUnit unit)
	{
		try
		{
			return _generalScheduledThreadPool.schedule(new RunnableWrapper(task), delay, unit);
		}
		catch (RejectedExecutionException e)
		{
			return null; /* shutdown, ignore */
		}
	}
	
	/**
	 * Schedules a general task to be executed at fixed rate.
	 * @param task the task to execute
	 * @param initialDelay the initial delay in milliseconds
	 * @param period the period between executions in milliseconds
	 * @return a ScheduledFuture representing pending completion of the task, and whose get() method will throw an exception upon cancellation
	 */
	public ScheduledFuture<?> scheduleGeneralAtFixedRate(Runnable task, long initialDelay, long period)
	{
		return scheduleGeneralAtFixedRate(task, initialDelay, period, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Schedules a general task to be executed at fixed rate.
	 * @param task the task to execute
	 * @param initialDelay the initial delay in the given time unit
	 * @param period the period between executions in the given time unit
	 * @param unit the time unit of the initialDelay and period parameters
	 * @return a ScheduledFuture representing pending completion of the task, and whose get() method will throw an exception upon cancellation
	 */
	public ScheduledFuture<?> scheduleGeneralAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit)
	{
		try
		{
			return _generalScheduledThreadPool.scheduleAtFixedRate(new RunnableWrapper(task), initialDelay, period, unit);
		}
		catch (RejectedExecutionException e)
		{
			return null; /* shutdown, ignore */
		}
	}
	
	public void shutdown()
	{
		_shutdown = true;
		
		try
		{
			_aiScheduledThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_effectsScheduledThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_eventsScheduledThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_generalScheduledThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			
			_eventsThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_generalPacketsThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_generalThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_ioPacketsThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			
			_aiScheduledThreadPool.shutdown();
			_effectsScheduledThreadPool.shutdown();
			_eventsScheduledThreadPool.shutdown();
			_generalScheduledThreadPool.shutdown();
			
			_eventsThreadPool.shutdown();
			_generalPacketsThreadPool.shutdown();
			_generalThreadPool.shutdown();
			_ioPacketsThreadPool.shutdown();
			
			LOG.info("{} are now stopped", getClass().getSimpleName());
		}
		catch (InterruptedException ie)
		{
			LOG.warn("There has been a problem shuting down the {}! {}", getClass().getSimpleName(), ie);
		}
	}
	
	private final class PriorityThreadFactory implements ThreadFactory
	{
		private final int _prio;
		private final String _name;
		private final AtomicInteger _threadNumber = new AtomicInteger(1);
		private final ThreadGroup _group;
		
		public PriorityThreadFactory(String name, int prio)
		{
			_prio = prio;
			_name = name;
			_group = new ThreadGroup(_name);
		}
		
		public ThreadGroup getGroup()
		{
			return _group;
		}
		
		@Override
		public Thread newThread(Runnable r)
		{
			final Thread t = new Thread(_group, r, _name + "-" + _threadNumber.getAndIncrement());
			t.setPriority(_prio);
			return t;
		}
	}
	
	private final class PurgeTask implements Runnable
	{
		private final ScheduledThreadPoolExecutor _aiScheduled;
		private final ScheduledThreadPoolExecutor _effectsScheduled;
		private final ScheduledThreadPoolExecutor _eventsScheduled;
		private final ScheduledThreadPoolExecutor _generalScheduled;
		
		private final ThreadPoolExecutor _events;
		private final ThreadPoolExecutor _generalPackets;
		private final ThreadPoolExecutor _general;
		private final ThreadPoolExecutor _ioPackets;
		
		public PurgeTask(ScheduledThreadPoolExecutor aiScheduledThreadPool, ScheduledThreadPoolExecutor effectsScheduledThreadPool, ScheduledThreadPoolExecutor eventsScheduledThreadPool, ScheduledThreadPoolExecutor generalScheduledThreadPool, //
			ThreadPoolExecutor eventsThreadPool, ThreadPoolExecutor generalPacketsThreadPool, ThreadPoolExecutor generalThreadPool, ThreadPoolExecutor ioPacketsThreadPool)
		{
			_aiScheduled = aiScheduledThreadPool;
			_effectsScheduled = effectsScheduledThreadPool;
			_eventsScheduled = eventsScheduledThreadPool;
			_generalScheduled = generalScheduledThreadPool;
			
			_events = eventsThreadPool;
			_generalPackets = generalPacketsThreadPool;
			_general = generalThreadPool;
			_ioPackets = ioPacketsThreadPool;
		}
		
		@Override
		public void run()
		{
			_aiScheduled.purge();
			_effectsScheduled.purge();
			_eventsScheduled.purge();
			_generalScheduled.purge();
			
			_events.purge();
			_generalPackets.purge();
			_general.purge();
			_ioPackets.purge();
		}
	}
	
	private final class RunnableWrapper implements Runnable
	{
		private final Runnable _runnable;
		
		public RunnableWrapper(Runnable runnable)
		{
			_runnable = runnable;
		}
		
		@Override
		public void run()
		{
			try
			{
				_runnable.run();
			}
			catch (final Throwable e)
			{
				final Thread t = Thread.currentThread();
				final UncaughtExceptionHandler h = t.getUncaughtExceptionHandler();
				if (h != null)
				{
					h.uncaughtException(t, e);
				}
			}
		}
	}
	
	private static final class SingletonHolder
	{
		@SuppressWarnings("synthetic-access")
		private static final ThreadPoolManager INSTANCE = new ThreadPoolManager();
	}
}