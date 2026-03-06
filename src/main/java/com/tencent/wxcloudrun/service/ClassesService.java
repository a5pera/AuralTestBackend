package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.dto.admin.ClassMaterialAccuracyDTO;
import com.tencent.wxcloudrun.dto.admin.ClassMaterialQuestionAccuracyDTO;
import com.tencent.wxcloudrun.dto.admin.ClassStudentCountDTO;
import com.tencent.wxcloudrun.dto.admin.ClassStudentDTO;
import com.tencent.wxcloudrun.model.user.Classes;

import java.util.List;

public interface ClassesService {
    Classes findById(Long id);

    List<Classes> listAllClasses();

    List<Classes> listActiveClasses();

    int activate(Long id);

    int deActive(Long id);

    int updateClassName(Long id, String name);

    List<ClassStudentDTO> listStudents(Long classId, Integer limit, Integer offset);

    ClassStudentCountDTO countStudents(Long classId);

    List<ClassMaterialAccuracyDTO> getMaterialAccuracy(Long classId);

    List<ClassMaterialQuestionAccuracyDTO> getMaterialQuestionAccuracy(Long classId, Long materialId);
}
