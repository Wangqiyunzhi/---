package com.service.impl;

import com.dao.GonggaoDao;
import com.entity.GonggaoEntity;
import com.service.GonggaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GonggaoServiceImpl implements GonggaoService {

    @Autowired
    private GonggaoDao gonggaoDao;

    @Override
    public List<GonggaoEntity> studentList() {
        return gonggaoDao.selectStudentList();
    }

    @Override
    public GonggaoEntity detail(Long id) {
        return gonggaoDao.selectById(id);
    }

    @Override
    public List<GonggaoEntity> teacherList(Long teacherId) {
        return gonggaoDao.selectTeacherList(teacherId);
    }

    @Override
    public List<GonggaoEntity> adminList() {
        return gonggaoDao.selectAdminList();
    }


    @Override
    public int teacherAdd(GonggaoEntity g, Long userId) {
        g.setId(null);
        g.setStatus(0);
        g.setPublisherRole("teacher");
        g.setPublisherId(userId);      // 必须是数字
        g.setPublishTime(null);
        return gonggaoDao.insert(g);
    }

    @Override
    public int adminAdd(GonggaoEntity g, Long userId) {
        g.setId(null);
        g.setStatus(0);
        g.setPublisherRole("admin");
        g.setPublisherId(userId);
        g.setPublishTime(null);
        return gonggaoDao.insert(g);
    }

    @Override
    public int teacherUpdate(GonggaoEntity g, Long userId) {
        g.setPublisherRole("teacher");
        g.setPublisherId(userId);
        return gonggaoDao.updateTeacherOwn(g);
    }

    @Override
    public int adminUpdate(GonggaoEntity g) {
        return gonggaoDao.updateById(g);
    }

    @Override
    public int teacherDelete(Long id, Long userId) {
        return gonggaoDao.deleteTeacherOwn(id, userId);
    }

    @Override
    public int adminDelete(Long id) {
        return gonggaoDao.deleteById(id);
    }

    @Override
    public int teacherPublish(Long id, Long userId) {
        return gonggaoDao.publishTeacherOwn(id, userId);
    }

    @Override
    public int adminPublish(Long id) {
        return gonggaoDao.publish(id);
    }

    @Override
    public int teacherUnpublish(Long id, Long userId) {
        return gonggaoDao.unpublishTeacherOwn(id, userId);
    }

    @Override
    public int adminUnpublish(Long id) {
        return gonggaoDao.unpublish(id);
    }
}
