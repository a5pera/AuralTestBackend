package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.dto.auth.StudentInfoDTO;

public interface StudentService {
    // 根据学生id获取学生信息
    StudentInfoDTO getStudentInfo(Long studentId);
}
