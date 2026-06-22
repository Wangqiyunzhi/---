package com.entity;

import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;

/**
 * 作业提交
 */
@TableName("zuoyetijiao")
public class ZuoyetijiaoEntity<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    public ZuoyetijiaoEntity() {
    }

    public ZuoyetijiaoEntity(T t) {
        try {
            BeanUtils.copyProperties(this, t);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @TableId
    private Long id;

    private String zuoyemingcheng;

    private String kemu;

    private String gonghao;

    private String jiaoshixingming;

    private String zuoyetupian;

    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat
    private Date tijiaoshijian;

    private String wanchengneirong;

    private String xuehao;

    private String xueshengxingming;

    private Long userid;

    private String pigaizhuangtai;

    private Double pingfen;

    private String pingyu;

    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat
    private Date pigaishijian;

    private String pigaigonghao;

    private String pigaijiaoshixingming;

    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat
    private Date addtime;

    public Date getAddtime() {
        return addtime;
    }

    public void setAddtime(Date addtime) {
        this.addtime = addtime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getZuoyemingcheng() {
        return zuoyemingcheng;
    }

    public void setZuoyemingcheng(String zuoyemingcheng) {
        this.zuoyemingcheng = zuoyemingcheng;
    }

    public String getKemu() {
        return kemu;
    }

    public void setKemu(String kemu) {
        this.kemu = kemu;
    }

    public String getGonghao() {
        return gonghao;
    }

    public void setGonghao(String gonghao) {
        this.gonghao = gonghao;
    }

    public String getJiaoshixingming() {
        return jiaoshixingming;
    }

    public void setJiaoshixingming(String jiaoshixingming) {
        this.jiaoshixingming = jiaoshixingming;
    }

    public String getZuoyetupian() {
        return zuoyetupian;
    }

    public void setZuoyetupian(String zuoyetupian) {
        this.zuoyetupian = zuoyetupian;
    }

    public Date getTijiaoshijian() {
        return tijiaoshijian;
    }

    public void setTijiaoshijian(Date tijiaoshijian) {
        this.tijiaoshijian = tijiaoshijian;
    }

    public String getWanchengneirong() {
        return wanchengneirong;
    }

    public void setWanchengneirong(String wanchengneirong) {
        this.wanchengneirong = wanchengneirong;
    }

    public String getXuehao() {
        return xuehao;
    }

    public void setXuehao(String xuehao) {
        this.xuehao = xuehao;
    }

    public String getXueshengxingming() {
        return xueshengxingming;
    }

    public void setXueshengxingming(String xueshengxingming) {
        this.xueshengxingming = xueshengxingming;
    }

    public Long getUserid() {
        return userid;
    }

    public void setUserid(Long userid) {
        this.userid = userid;
    }

    public String getPigaizhuangtai() {
        return pigaizhuangtai;
    }

    public void setPigaizhuangtai(String pigaizhuangtai) {
        this.pigaizhuangtai = pigaizhuangtai;
    }

    public Double getPingfen() {
        return pingfen;
    }

    public void setPingfen(Double pingfen) {
        this.pingfen = pingfen;
    }

    public String getPingyu() {
        return pingyu;
    }

    public void setPingyu(String pingyu) {
        this.pingyu = pingyu;
    }

    public Date getPigaishijian() {
        return pigaishijian;
    }

    public void setPigaishijian(Date pigaishijian) {
        this.pigaishijian = pigaishijian;
    }

    public String getPigaigonghao() {
        return pigaigonghao;
    }

    public void setPigaigonghao(String pigaigonghao) {
        this.pigaigonghao = pigaigonghao;
    }

    public String getPigaijiaoshixingming() {
        return pigaijiaoshixingming;
    }

    public void setPigaijiaoshixingming(String pigaijiaoshixingming) {
        this.pigaijiaoshixingming = pigaijiaoshixingming;
    }
}
