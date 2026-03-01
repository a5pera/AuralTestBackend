package com.tencent.wxcloudrun.service.impl;

import com.tencent.wxcloudrun.dao.ClassesMapper;
import com.tencent.wxcloudrun.model.user.Classes;
import com.tencent.wxcloudrun.service.ClassesService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClassesServiceImpl implements ClassesService {
    private final ClassesMapper classesMapper;

    public ClassesServiceImpl(ClassesMapper classesMapper) {
        this.classesMapper = classesMapper;
    }


    @Override
    public Classes findById(Long id) {
        if (id == null) throw new IllegalArgumentException("MISSING_PARAM");
        return classesMapper.findById(id);
    }

    @Override
    public List<Classes> listAllClasses() {
        return classesMapper.listAll();
    }

    @Override
    public List<Classes> listActiveClasses() {
        return classesMapper.listActive();
    }

    @Override
    public int activate(Long id) {
        if (id == null) throw new IllegalArgumentException("MISSING_PARAM");
        return classesMapper.activate(id);
    }

    @Override
    public int deActive(Long id) {
        if (id == null) throw new IllegalArgumentException("MISSING_PARAM");
        return classesMapper.deActivate(id);
    }
}
