package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.dto.admin.StudentPracticeReportDTO;

public interface AdminPracticeService {
    StudentPracticeReportDTO getStudentPracticeReport(Long studentId);
}
