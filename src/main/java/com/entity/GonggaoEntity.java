package com.entity;

import java.io.Serializable;

public class GonggaoEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String content;
    private Integer status;
    private String publisherRole;
    private Long publisherId;
    private Long addtime;
    private Long publishTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getPublisherRole() { return publisherRole; }
    public void setPublisherRole(String publisherRole) { this.publisherRole = publisherRole; }

    public Long getPublisherId() { return publisherId; }
    public void setPublisherId(Long publisherId) { this.publisherId = publisherId; }

    public Long getAddtime() { return addtime; }
    public void setAddtime(Long addtime) { this.addtime = addtime; }

    public Long getPublishTime() { return publishTime; }
    public void setPublishTime(Long publishTime) { this.publishTime = publishTime; }
}
