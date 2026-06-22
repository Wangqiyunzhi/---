package com.entity.view;

import com.baomidou.mybatisplus.annotations.TableName;
import com.entity.GonggaoEntity;
import org.apache.commons.beanutils.BeanUtils;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

/**
 * 公告信息
 * 后端返回用 View（保持和项目其他 View 一致）
 */
@TableName("gonggao")
public class GonggaoView extends GonggaoEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    public GonggaoView() {
    }

    public GonggaoView(GonggaoEntity entity) {
        try {
            BeanUtils.copyProperties(this, entity);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
