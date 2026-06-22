package com.dao;

import com.entity.GonggaoEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GonggaoDao {

    int insert(GonggaoEntity g);

    int updateById(GonggaoEntity g);

    int deleteById(@Param("id") Long id);

    GonggaoEntity selectById(@Param("id") Long id);

    List<GonggaoEntity> selectStudentList();

    List<GonggaoEntity> selectTeacherList(@Param("publisherId") Long publisherId);

    List<GonggaoEntity> selectAdminList();

    int publish(@Param("id") Long id);

    int unpublish(@Param("id") Long id);

    int deleteTeacherOwn(@Param("id") Long id, @Param("publisherId") Long publisherId);

    int updateTeacherOwn(GonggaoEntity g); // 通过xml里 where 限制 owner
    int publishTeacherOwn(@Param("id") Long id, @Param("publisherId") Long publisherId);
    int unpublishTeacherOwn(@Param("id") Long id, @Param("publisherId") Long publisherId);
}
