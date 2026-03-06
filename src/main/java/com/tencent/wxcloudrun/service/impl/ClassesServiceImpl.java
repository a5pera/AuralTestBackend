package com.tencent.wxcloudrun.service.impl;

import com.tencent.wxcloudrun.dao.ClassesMapper;
import com.tencent.wxcloudrun.dao.StudentRosterMapper;
import com.tencent.wxcloudrun.dto.admin.ClassMaterialAccuracyDTO;
import com.tencent.wxcloudrun.dto.admin.ClassMaterialQuestionAccuracyDTO;
import com.tencent.wxcloudrun.dto.admin.ClassStudentCountDTO;
import com.tencent.wxcloudrun.dto.admin.ClassStudentDTO;
import com.tencent.wxcloudrun.model.auth.StudentRoster;
import com.tencent.wxcloudrun.model.user.Classes;
import com.tencent.wxcloudrun.service.ClassesService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClassesServiceImpl implements ClassesService {
    private final ClassesMapper classesMapper;
    private final StudentRosterMapper studentRosterMapper;

    public ClassesServiceImpl(ClassesMapper classesMapper,
                              StudentRosterMapper studentRosterMapper) {
        this.classesMapper = classesMapper;
        this.studentRosterMapper = studentRosterMapper;
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

    @Override
    public int updateClassName(Long id, String name) {
        if(id == null || name == null) throw new IllegalArgumentException("MISSING_PARAM");
        return classesMapper.updateName(id, name);
    }

    @Override
    public List<ClassStudentDTO> listStudents(Long classId, Integer limit, Integer offset) {
        if (classId == null) throw new IllegalArgumentException("CLASS_ID_MISSING");
        int lim = (limit == null ? 50 : Math.max(1, Math.min(limit, 200)));
        int off = (offset == null ? 0 : Math.max(0, offset));
        return studentRosterMapper.listClassStudents(classId, lim, off);
    }

    @Override
    public ClassStudentCountDTO countStudents(Long classId) {
        if(classId == null) throw new IllegalArgumentException("CLASS_ID_MISSING");
        return studentRosterMapper.countClassStudents(classId);
    }

    @Override
    public List<ClassMaterialAccuracyDTO> getMaterialAccuracy(Long classId) {
        if(classId == null) throw new IllegalArgumentException("CLASS_ID_MISSING");
        return classesMapper.statMaterialAccuracyByClassLatest(classId);
    }

    @Override
    public List<ClassMaterialQuestionAccuracyDTO> getMaterialQuestionAccuracy(Long classId, Long materialId) {
        if(classId == null || materialId == null) throw new IllegalArgumentException("MISSING_PARAM");
        return classesMapper.statQuestionAccuracyByClassLatestAttempt(classId, materialId);
    }
}
