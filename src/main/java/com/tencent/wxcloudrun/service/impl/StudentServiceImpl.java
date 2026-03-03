package com.tencent.wxcloudrun.service.impl;

import com.tencent.wxcloudrun.dao.ClassesMapper;
import com.tencent.wxcloudrun.dao.StudentMapper;
import com.tencent.wxcloudrun.dto.auth.StudentInfoDTO;
import com.tencent.wxcloudrun.model.auth.Student;
import com.tencent.wxcloudrun.model.user.Classes;
import com.tencent.wxcloudrun.service.StudentService;
import org.springframework.stereotype.Service;

@Service
public class StudentServiceImpl implements StudentService{
    private final StudentMapper studentMapper;
    private final ClassesMapper classesMapper;
    public StudentServiceImpl(StudentMapper studentMapper,
                              ClassesMapper classesMapper) {
        this.studentMapper = studentMapper;
        this.classesMapper = classesMapper;
    }


    @Override
    public StudentInfoDTO getStudentInfo(Long studentId) {
        Student s = studentMapper.findByIntegerId(studentId);
        StudentInfoDTO out = new StudentInfoDTO();
        out.setId(s.getId());
        out.setName(s.getName());
        out.setCollege(s.getCollege());
        out.setClassId(s.getClassId());
        Classes clazz = classesMapper.findById(s.getClassId());
        out.setClassName(clazz.getName());
        return out;
    }
}

