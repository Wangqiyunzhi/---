package com.service;

import com.entity.GonggaoEntity;

import java.util.List;

public interface GonggaoService {

    List<GonggaoEntity> studentList();
    GonggaoEntity detail(Long id);

    List<GonggaoEntity> teacherList(Long teacherId);
    List<GonggaoEntity> adminList();

    int teacherAdd(GonggaoEntity g, Long userId);
    int adminAdd(GonggaoEntity g, Long userId);

    int teacherUpdate(GonggaoEntity g, Long userId);
    int adminUpdate(GonggaoEntity g);

    int teacherDelete(Long id, Long userId);
    int adminDelete(Long id);

    int teacherPublish(Long id, Long userId);
    int adminPublish(Long id);

    int teacherUnpublish(Long id, Long userId);
    int adminUnpublish(Long id);
}
