package com.tencent.wxcloudrun.service.impl;

import com.tencent.wxcloudrun.dao.AdminPracticeMapper;
import com.tencent.wxcloudrun.dao.StudentMapper;
import com.tencent.wxcloudrun.dto.admin.StudentPracticeReportDTO;
import com.tencent.wxcloudrun.dto.db.StudentPracticeRow;
import com.tencent.wxcloudrun.model.auth.Student;
import com.tencent.wxcloudrun.service.AdminPracticeService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class AdminPracticeServiceImpl implements AdminPracticeService {
    private final AdminPracticeMapper adminPracticeMapper;
    private final StudentMapper studentMapper;

    public AdminPracticeServiceImpl(AdminPracticeMapper adminPracticeMapper,
                                StudentMapper studentMapper) {
        this.adminPracticeMapper = adminPracticeMapper;
        this.studentMapper = studentMapper;
    }

    public StudentPracticeReportDTO getStudentPracticeReport(Long studentId) {
        if (studentId == null) throw new IllegalArgumentException("STUDENT_ID_MISSING");

        // 兜底：即使该学生没练过，也希望返回 student 基本信息
        Student stu = studentMapper.findById(String.valueOf(studentId));
        if (stu == null) throw new IllegalArgumentException("STUDENT_NOT_FOUND");

        List<StudentPracticeRow> rows = adminPracticeMapper.listLatestPracticeRowsByStudentId(studentId);

        StudentPracticeReportDTO out = new StudentPracticeReportDTO();
        out.setStudentId(studentId);
        out.setStudentNo(stu.getStudentNo());
        out.setStudentName(stu.getName());

        if (rows == null || rows.isEmpty()) {
            out.setMaterials(List.of());
            return out;
        }

        // materialId -> material DTO
        Map<Long, StudentPracticeReportDTO.MaterialPracticeDTO> mMap = new LinkedHashMap<>();
        // materialId -> [correctCount, totalCount]
        Map<Long, long[]> statMap = new HashMap<>();

        for (StudentPracticeRow r : rows) {
            if (r.getMaterialId() == null) continue;

            StudentPracticeReportDTO.MaterialPracticeDTO mDto =
                    mMap.computeIfAbsent(r.getMaterialId(), mid -> {
                        StudentPracticeReportDTO.MaterialPracticeDTO x = new StudentPracticeReportDTO.MaterialPracticeDTO();
                        x.setMaterialId(r.getMaterialId());
                        x.setMaterialTitle(r.getMaterialTitle());
                        x.setQuestions(new ArrayList<>());
                        x.setAccuracy(BigDecimal.ZERO);
                        statMap.put(mid, new long[]{0, 0});
                        return x;
                    });

            StudentPracticeReportDTO.QuestionAnswerDTO qdto = new StudentPracticeReportDTO.QuestionAnswerDTO();
            qdto.setQuestionId(r.getQuestionId());
            qdto.setQOrder(r.getQOrder());
            qdto.setCorrectKey(normKey(r.getCorrectKey()));
            qdto.setSelectedKey(normKey(r.getSelectedKey()));
            qdto.setIsCorrect(r.getIsCorrect() != null && r.getIsCorrect() == 1);

            mDto.getQuestions().add(qdto);

            long[] st = statMap.get(r.getMaterialId());
            st[1]++; // total++
            if (qdto.getIsCorrect() != null && qdto.getIsCorrect()) st[0]++; // correct++
        }

        // 计算每个材料 accuracy
        for (Map.Entry<Long, StudentPracticeReportDTO.MaterialPracticeDTO> e : mMap.entrySet()) {
            long[] st = statMap.get(e.getKey());
            long correct = st[0], total = st[1];
            BigDecimal acc = (total == 0)
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(correct)
                    .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
            e.getValue().setAccuracy(acc);
        }

        out.setMaterials(new ArrayList<>(mMap.values()));
        return out;
    }

    private static String normKey(String k) {
        if (k == null) return null;
        String s = k.trim();
        return s.isEmpty() ? null : s.toUpperCase();
    }
}
