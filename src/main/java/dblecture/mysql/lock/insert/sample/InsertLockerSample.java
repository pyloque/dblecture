package dblecture.mysql.lock.insert.sample;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ibatis.logging.LogFactory;

import dblecture.mysql.MySQLStore;
import dblecture.mysql.lock.insert.InsertLocker;
import dblecture.mysql.lock.insert.LockerMapper;

public class InsertLockerSample {

	public static void main(String[] args) {
		// docker run -p3306:3306 --name mysql -e MYSQL_DATABASE=locker -e
		// MYSQL_ROOT_PASSWORD=123456 mysql:5.7
		LogFactory.useNoLogging();
		MySQLStore store = new MySQLStore("localhost", 3306, "locker", "root", "123456");
		store.prepare(factory -> {
			factory.getConfiguration().addMapper(LockerMapper.class);
		});
		InsertLocker locker = new InsertLocker(store);
		locker.dropTable();
		locker.createTable();
		System.out.println("*******************");
		testSequencially(locker);
		System.out.println("*******************");
		testConcurrent(locker);
		store.close();
	}

	private static void testSequencially(InsertLocker locker) {
		AtomicInteger success = new AtomicInteger(0);
		AtomicInteger runs = new AtomicInteger(0);
		for (int i = 0; i < 100; i++) {
			boolean got = locker.with("a", 5, () -> {
				try {
					runs.incrementAndGet();
					Thread.sleep(2);
				} catch (InterruptedException e) {
				}
			});
			if (got) {
				success.incrementAndGet();
			}
		}
		if (success.intValue() == runs.intValue()) {
			System.out.printf("testSequencially is ok with success=%d runs=%d\n", success.intValue(), runs.intValue());
		} else {
			System.out.printf("testSequencially is not ok with success=%d runs=%d\n", success.intValue(),
					runs.intValue());
		}
	}

	private static void testConcurrent(InsertLocker locker) {
		ExecutorService executor = Executors.newFixedThreadPool(10);
		String[] ids = new String[] { "a", "b", "c", "d", "e", "f", "g" };
		AtomicInteger success = new AtomicInteger(0);
		AtomicInteger runs = new AtomicInteger(0);
		for (int i = 0; i < 1000; i++) {
			executor.submit(() -> {
				String id = ids[ThreadLocalRandom.current().nextInt(ids.length)];
				boolean got = locker.with(id, 5, () -> {
					try {
						runs.incrementAndGet();
						Thread.sleep(2);
					} catch (InterruptedException e) {
					}
				});
				if (got) {
					success.incrementAndGet();
				}
			});
		}
		executor.shutdown();
		try {
			executor.awaitTermination(100, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}
		if (success.intValue() == runs.intValue()) {
			System.out.printf("testConcurrent is ok with success=%d runs=%d\n", success.intValue(), runs.intValue());
		} else {
			System.out.printf("testConcurrent is not ok with success=%d runs=%d\n", success.intValue(),
					runs.intValue());
		}
	}

}
