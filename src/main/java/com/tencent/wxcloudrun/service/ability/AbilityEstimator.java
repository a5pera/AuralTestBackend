package com.tencent.wxcloudrun.service.ability;

import com.tencent.wxcloudrun.model.quest.Question;

import java.util.List;
import java.util.Map;

public final class AbilityEstimator {

    // 能力范围（和你系统的 1~10 量表一致）
    private static final double THETA_MIN = 1.0;
    private static final double THETA_MAX = 10.0;

    // 题目区分度/噪声尺度（越小越“陡”，更新更激进；越大越平滑）
    private static final double BETA = 1.0;

    // 过程噪声：让系统在新材料下仍可微调（否则 var 会快速变得极小导致“锁死”）
    private static final double PROCESS_NOISE_Q = 0.08; // 建议 0.05~0.15 之间试

    // 单次更新最大步长（防止跳太大；想 20 个材料左右收敛，通常 0.6~1.0 合理）
    private static final double STEP_CAP = 0.8;

    private AbilityEstimator() {
    }

    public static class UpdateResult {
        public final double thetaNew;
        public final double varNew;

        public UpdateResult(double thetaNew, double varNew) {
            this.thetaNew = thetaNew;
            this.varNew = varNew;
        }
    }

    private static double sigmoid(double x) {
        x = Math.max(-20.0, Math.min(20.0, x));
        return 1.0 / (1.0 + Math.exp(-x));
    }

    private static double clip(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /**
     * 一次材料提交，更新 (theta, var)
     * prior: N(theta_t, var_t + q)
     * likelihood: 题目 logistic IRT: p = sigmoid((theta - d)/beta)
     * Laplace: var_{t+1} ≈ 1 / (priorPrec + observedInfo(theta_{t+1}))
     */
    public static UpdateResult update(double theta,
                                      double var,
                                      List<Question> questions,
                                      Map<Long, Boolean> correctByQid) {

        double mu0 = clip(theta, THETA_MIN, THETA_MAX);
        double priorVar = Math.max(1e-6, var + PROCESS_NOISE_Q);
        double priorPrec = 1.0 / priorVar;

        // 牛顿一步（你也可以循环 2~3 次，但一般一次材料更新一步就够稳）
        double g = 0.0;   // d/dtheta log posterior
        double negH = 0.0; // -(d^2/dtheta^2 log posterior)  > 0

        for (Question q : questions) {
            if (q == null || q.getDifficulty() == null) continue;
            double d = q.getDifficulty().doubleValue();
            double p = sigmoid((mu0 - d) / BETA);
            double y = Boolean.TRUE.equals(correctByQid.get(q.getId())) ? 1.0 : 0.0;

            // loglik grad/hess
            g += (y - p) / BETA;
            negH += (p * (1.0 - p)) / (BETA * BETA);
        }

        // prior term: -(theta-mu0)^2/(2 priorVar)
        // grad += -(theta-mu0)/priorVar;  这里 theta=mu0，所以这一项为 0（顺序贝叶斯：以上一次为先验均值）
        // hess adds -1/priorVar => negH += 1/priorVar
        negH += priorPrec;

        double step = g / Math.max(negH, 1e-8);
        step = clip(step, -STEP_CAP, STEP_CAP);

        double thetaNew = clip(mu0 + step, THETA_MIN, THETA_MAX);

        // 用 thetaNew 重新算观测信息，做 Laplace 方差
        double info = 0.0;
        for (Question q : questions) {
            if (q == null || q.getDifficulty() == null) continue;
            double d = q.getDifficulty().doubleValue();
            double p = sigmoid((thetaNew - d) / BETA);
            info += (p * (1.0 - p)) / (BETA * BETA);
        }
        double varNew = 1.0 / Math.max(priorPrec + info, 1e-8);

        // 防止 var 过小导致后续几乎不更新（可选下限）
        varNew = Math.max(varNew, 0.05);

        return new UpdateResult(thetaNew, varNew);
    }
}
