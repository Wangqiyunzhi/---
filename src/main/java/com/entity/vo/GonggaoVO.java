package com.entity.vo;

import java.io.Serializable;
import java.util.Date;

/**
 * 公告信息
 * 手机端返回用 VO（和 Entity 字段保持一致即可）
 */
public class GonggaoVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String content;
    private Integer status;
    private String publisherRole;
    private String publisherId;
    private Date addtime;
    private Date publishTime;

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

    public String getPublisherId() { return publisherId; }
    public void setPublisherId(String publisherId) { this.publisherId = publisherId; }

    public Date getAddtime() { return addtime; }
    public void setAddtime(Date addtime) { this.addtime = addtime; }

    public Date getPublishTime() { return publishTime; }
    public void setPublishTime(Date publishTime) { this.publishTime = publishTime; }
}
