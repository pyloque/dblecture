package dblecture.mysql.queue;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import dblecture.mysql.Holder;
import dblecture.mysql.MySQLStore;

public class TaskQueue<T> {

	private final static Logger LOG = LoggerFactory.getLogger(TaskQueue.class);

	private MySQLStore store;
	private ExecutorService executor;
	private String queueName;
	private Class<T> valueClass;

	public TaskQueue(MySQLStore store, String queueName, Class<T> valueClass, int concurrent) {
		this.store = store;
		this.executor = Executors.newFixedThreadPool(concurrent);
		this.queueName = queueName;
		this.valueClass = valueClass;
		this.store.prepare(factory -> {
			factory.getConfiguration().addMapper(TaskMapper.class);
		});
	}

	public void createTable() {
		this.store.executeWithMapper(TaskMapper.class, mapper -> {
			mapper.createTable(queueName);
		});
	}

	public void dropTable() {
		this.store.executeWithMapper(TaskMapper.class, mapper -> {
			mapper.dropTable(queueName);
		});
	}

	@SuppressWarnings("unchecked")
	public void push(T... items) {
		this.delay(0, items);
	}

	public int size() {
		Holder<Integer> holder = new Holder<>();
		this.store.executeWithMapper(TaskMapper.class, mapper -> {
			holder.value(mapper.size(queueName));
		});
		return holder.value();
	}

	@SuppressWarnings("unchecked")
	public void delay(int seconds, T... items) {
		this.store.executeWithMapper(TaskMapper.class, mapper -> {
			for (T item : items) {
				TaskItem task = new TaskItem(UUID.randomUUID().toString(), JSON.toJSONString(item),
						System.currentTimeMillis() + seconds * 1000);
				mapper.insertTask(queueName, task);
			}
		});
	}

	public T poll() {
		Holder<T> holder = new Holder<>();
		this.store.executeWithMapper(TaskMapper.class, mapper -> {
			int maxRetries = 5;
			while (maxRetries-- > 0) {
				// 先拿出来
				TaskItem task = mapper.pollTask(queueName, System.currentTimeMillis());
				if (task == null) {
					return;
				}
				// 再删除
				if (mapper.deleteTask(queueName, task) > 0) {
					holder.value(JSON.parseObject(task.getValue(), valueClass));
					return;
				}
			}
		});
		return holder.value();
	}

	private Thread thread;
	private CountDownLatch stopSignal = new CountDownLatch(1);

	public void start(Consumer<T> consumer) {
		this.thread = new Thread() {
			public void run() {
				try {
					while (!Thread.interrupted()) {
						T task = null;
						try {
							task = poll();
						} catch (Exception e) {
							LOG.error("poll task error", e);
						}
						if (task == null) {
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								break;
							}
							continue;
						}
						try {
							consumer.accept(task);
						} catch (Exception e) {
							LOG.error("task process error", e);
						}
					}
				} finally {
					stopSignal.countDown();
				}
			}
		};
		this.thread.start();
	}

	public void stop() {
		this.thread.interrupt();
		try {
			this.stopSignal.await(5000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
		}
		this.executor.shutdown();
		try {
			if (!this.executor.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
				this.executor.shutdownNow();
			}
		} catch (InterruptedException e) {
		}
	}

}
