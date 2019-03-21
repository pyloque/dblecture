package dblecture.mysql.lock.forupdate.sample;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ibatis.logging.LogFactory;

import dblecture.mysql.MySQLStore;
import dblecture.mysql.lock.forupdate.ForUpdateLocker;
import dblecture.mysql.lock.forupdate.LockerMapper;

public class ForUpdateLockerSample {

	public static void main(String[] args) {
		// docker run -p3306:3306 --name mysql -e MYSQL_DATABASE=locker -e
		// MYSQL_ROOT_PASSWORD=123456 mysql:5.7
		LogFactory.useNoLogging();
		MySQLStore store = new MySQLStore("localhost", 3306, "locker", "root", "123456");
		store.prepare(factory -> {
			factory.getConfiguration().addMapper(LockerMapper.class);
		});
		ForUpdateLocker locker = new ForUpdateLocker(store, 10);
		locker.dropTable();
		locker.createTableAndLocks();
		System.out.println("*******************");
		testSequencially(locker);
		System.out.println("*******************");
		testConcurrent(locker);
		System.out.println("*******************");
		testMultiLocks(locker);
		store.close();
	}

	private static void testSequencially(ForUpdateLocker locker) {
		AtomicInteger runs = new AtomicInteger(0);
		for (int i = 0; i < 100; i++) {
			locker.with("a", () -> {
				try {
					runs.incrementAndGet();
					Thread.sleep(2);
				} catch (InterruptedException e) {
				}
			});
		}
		System.out.printf("testSequencially runs %d times\n", runs.get());
	}

	private static void testConcurrent(ForUpdateLocker locker) {
		ExecutorService executor = Executors.newFixedThreadPool(100);
		AtomicInteger runs = new AtomicInteger(0);
		for (int i = 0; i < 1000; i++) {
			String id = "id" + i;
			executor.submit(() -> {
				locker.with(id, () -> {
					try {
						runs.incrementAndGet();
						Thread.sleep(2);
					} catch (InterruptedException e) {
					}
				});
			});
		}
		executor.shutdown();
		try {
			executor.awaitTermination(100, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}
		System.out.printf("testConcurrent runs %d times\n", runs.get());
	}

	private static void testMultiLocks(ForUpdateLocker locker) {
		ExecutorService executor = Executors.newFixedThreadPool(10);
		AtomicInteger runs = new AtomicInteger(0);
		for (int i = 0; i < 1000; i++) {
			String id1 = "id" + ThreadLocalRandom.current().nextInt(100);
			String id2 = "id" + ThreadLocalRandom.current().nextInt(100);
			String id3 = "id" + ThreadLocalRandom.current().nextInt(100);
			executor.submit(() -> {
				locker.withMulti(Arrays.asList(id1, id2, id3), () -> {
					try {
						runs.incrementAndGet();
						Thread.sleep(2);
					} catch (InterruptedException e) {
					}
				});
			});
		}
		executor.shutdown();
		try {
			executor.awaitTermination(100, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}
		System.out.printf("testMultiLocks runs %d times\n", runs.get());
	}

}
