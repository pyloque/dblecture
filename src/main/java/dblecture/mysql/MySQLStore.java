package dblecture.mysql;

import java.sql.SQLException;
import java.util.function.Consumer;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MySQLStore {
	private final static Logger LOG = LoggerFactory.getLogger(MySQLStore.class);

	private PooledDataSource ds;
	private SqlSessionFactory factory;

	public MySQLStore(String host, int port, String db, String user, String passwd) {
		String driver = "com.mysql.cj.jdbc.Driver";
		String url = String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=UTF8&useSSL=false", host, port, db);
		this.ds = new PooledDataSource(driver, url, user, passwd);
		ds.setPoolMaximumActiveConnections(10);
		ds.setPoolPingEnabled(true);
		ds.setPoolPingQuery("select 1");
		ds.setPoolPingConnectionsNotUsedFor(10000);
		TransactionFactory trxFactory = new JdbcTransactionFactory();
		Environment env = new Environment("wtf", trxFactory, ds);
		Configuration c = new Configuration(env);
		c.setCacheEnabled(false);
		this.factory = new SqlSessionFactoryBuilder().build(c);
	}

	public void prepare(Consumer<SqlSessionFactory> prepareCallback) {
		prepareCallback.accept(factory);
	}

	public void execute(MySQLOperation<SqlSession> consumer) {
		this.execute(consumer, true);
	}

	public <T> void executeWithMapper(Class<T> mapperClass, MySQLOperation<T> consumer) {
		this.executeWithMapper(mapperClass, consumer, true);
	}

	public <T> void executeWithMapper(Class<T> mapperClass, MySQLOperation<T> consumer, boolean autocommit) {
		this.execute(session -> {
			T mapper = session.getMapper(mapperClass);
			consumer.accept(mapper);
		}, autocommit);
	}

	public void execute(MySQLOperation<SqlSession> consumer, boolean autocommit) {
		SqlSession session;
		try {
			session = factory.openSession(autocommit);
		} catch (Exception e) {
			LOG.error("connect mysql error", e);
			throw new RuntimeException("connect mysql error", e);
		}
		try {
			consumer.accept(session);
		} catch (SQLException e) {
			if (!autocommit)
				session.rollback();
			LOG.error("access mysql error", e);
			throw new RuntimeException("access mysql error", e);
		} finally {
			session.close();
		}
	}

	public void close() {
		this.ds.forceCloseAll();
	}
}
