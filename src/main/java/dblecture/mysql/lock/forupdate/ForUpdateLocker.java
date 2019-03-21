package dblecture.mysql.lock.forupdate;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.CRC32;

import dblecture.mysql.MySQLStore;

public class ForUpdateLocker {

	private MySQLStore mysql;
	private int concurrent;

	// 记录当前持有的锁id，不允许乱序加锁，避免死锁
	// 不允许重入锁
	private ThreadLocal<Set<Integer>> lockingIds = new ThreadLocal<>();

	public ForUpdateLocker(MySQLStore mysql, int concurrent) {
		this.mysql = mysql;
		this.concurrent = concurrent;
	}

	public void createTableAndLocks() {
		this.mysql.executeWithMapper(LockerMapper.class, mapper -> {
			mapper.createTable();
			for (int i = 0; i < concurrent; i++) {
				mapper.insertLocker(new Locker("l" + i, System.currentTimeMillis()));
			}
		});
	}

	public void dropTable() {
		this.mysql.executeWithMapper(LockerMapper.class, mapper -> {
			mapper.dropTable();
		});
	}

	public void with(String id, Runnable op) {
		// 获取 id 对应的锁 id
		int lockId = this.getLockId(id);
		this.lockWithLockIds(new int[] { lockId }, op);
	}

	private void lockWithLockIds(int[] lockIds, Runnable op) {
		// 检查锁 id 的有序性
		Set<Integer> ids = lockingIds.get();
		for (int lockId : lockIds) {
			if (ids == null) {
				ids = new HashSet<>();
				lockingIds.set(ids);
			} else {
				for (int lid : ids) {
					if (lid >= lockId) {
						throw new RuntimeException("lock id must be ordered");
					}
				}
			}
			ids.add(lockId);
		}
		try {
			this.mysql.execute(session -> {
				LockerMapper mapper = session.getMapper(LockerMapper.class);
				for (int lockId : lockIds) {
					Locker locker = new Locker("l" + lockId, System.currentTimeMillis());
					mapper.forUpdate(locker.getId());
					mapper.updateLocker(locker);
				}
				try {
					op.run();
				} finally {
					session.commit();
				}
			});
		} finally {
			for (int lockId : lockIds) {
				ids.remove(lockId);
				if (ids.isEmpty()) {
					lockingIds.remove();
				}
			}
		}
	}

	public int getLockId(String id) {
		CRC32 crc = new CRC32();
		crc.update(id.getBytes(UTF8));
		return (int) (Math.abs(crc.getValue()) % concurrent);
	}

	public void withMulti(String[] ids, Runnable op) {
		// 先对锁进行排序
		Set<Integer> uniqueIds = new HashSet<>();
		for (String id : ids) {
			uniqueIds.add(getLockId(id));
		}
		int[] lockIds = new int[uniqueIds.size()];
		int i = 0;
		for (int id : uniqueIds) {
			lockIds[i++] = id;
		}
		Arrays.sort(lockIds);
		this.lockWithLockIds(lockIds, op);
	}

	public void withMulti(List<String> ids, Runnable op) {
		String[] arr = new String[ids.size()];
		ids.toArray(arr);
		this.withMulti(arr, op);
	}

	private final static Charset UTF8 = Charset.forName("UTF-8");
}
