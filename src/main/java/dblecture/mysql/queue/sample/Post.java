package dblecture.mysql.queue.sample;

import com.alibaba.fastjson.annotation.JSONField;

public class Post {

	private String id;
	@JSONField(name = "user_id")
	private String userId;
	private String title;
	private String content;

	public Post() {
	}

	public Post(String id, String userId, String title, String content) {
		this.id = id;
		this.userId = userId;
		this.title = title;
		this.content = content;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

}
