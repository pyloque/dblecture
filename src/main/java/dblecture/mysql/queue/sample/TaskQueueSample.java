package dblecture.mysql.queue.sample;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ibatis.logging.LogFactory;

import dblecture.mysql.MySQLStore;
import dblecture.mysql.queue.TaskQueue;

public class TaskQueueSample {

	public static void main(String[] args) {
		// docker run -p3306:3306 --name mysql -e MYSQL_DATABASE=locker -e
		// MYSQL_ROOT_PASSWORD=123456 mysql:5.7
		LogFactory.useNoLogging();
		MySQLStore store = new MySQLStore("localhost", 3306, "locker", "root", "123456");
		TaskQueue<Post> queue = new TaskQueue<>(store, "post", Post.class, 4);
		queue.dropTable();
		queue.createTable();

		AtomicInteger consumes = new AtomicInteger();

		queue.start(post -> {
			consumes.incrementAndGet();
		});

		for (int i = 0; i < 100; i++) {
			queue.push(new Post("" + i, "123", "test-title", "test-content"));
		}

		System.out.println(queue.size());

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
		}

		System.out.println(queue.size());

		System.out.printf("task queue consume %d tasks\n", consumes.get());
		queue.stop();
	}

}
