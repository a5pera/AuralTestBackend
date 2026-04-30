# 听音测试系统 - 能力评估核心算法说明 (Ability Estimator)

本文档说明了项目中用于评估学生听音能力的算法原理。算法实现在 `src/main/java/com/tencent/wxcloudrun/service/ability/AbilityEstimator.java`。

## 1. 算法背景与目的

该算法主要用于在学生完成一次练习材料（包含多道题目）后，动态、平滑地更新学生的“能力值”和“方差（不确定度）”。算法基于**项目反应理论 (Item Response Theory, IRT)** 和 **贝叶斯更新 (Bayesian Updating)** 相结合的思想。

其目标是：
*   根据学生答题的对错情况，合理地增减其能力评估值。
*   考虑题目的难度，答对难题加分多，答错简单题扣分多。
*   通过“方差”来追踪系统对该学生能力评估的确定程度，随着做题数量增加，系统对其能力的评估越来越准确（方差变小）。
*   能够适应学生能力的缓慢变化，避免早期数据将能力值“锁死”。

## 2. 核心数学模型

### 2.1 基础假设：IRT Logistic 模型

假设学生的能力值为 $\theta$（Theta），题目的难度为 $d$（Difficulty）。学生答对这道题的概率 $P$ 服从 Logistic 函数（Sigmoid 曲线）：

$$P(\theta, d) = \sigma\left(\frac{\theta - d}{\beta}\right) = \frac{1}{1 + e^{-(\theta - d)/\beta}}$$

*   $\theta$ (Theta): 学生的能力值，限定范围 `[THETA_MIN, THETA_MAX]` (代码中为 1.0 ~ 10.0)。
*   $d$ (Difficulty): 题目的难度，与能力值在同一量表上。
*   $\beta$ (BETA): 区分度/缩放因子。代码中设为 `1.0`。$\beta$ 越小，曲线越陡峭（区分度越高）；$\beta$ 越大，概率变化越平缓。

### 2.2 贝叶斯更新过程

当学生提交一次练习后，系统使用贝叶斯推断结合牛顿法（Newton-Raphson method）的单步迭代来更新先验分布 $P(\theta)$。

**步骤 1: 引入过程噪声 (Process Noise)**

为了防止随着练习次数增多，先验方差过小导致能力值僵化（无法反映学生能力的实际提升或下降），在更新前会人为增加一点方差：

$$V_{prior} = \max(10^{-6}, V_{current} + Q)$$

其中 $Q$ (`PROCESS_NOISE_Q` = 0.08) 是过程噪声。增加噪声后，先验精度 (Precision) 为 $Prec_{prior} = 1 / V_{prior}$。

**步骤 2: 计算对数似然的一阶导 (Gradient) 和二阶导 (Hessian 的相反数)**

对于本次提交的所有题目，根据学生实际答题结果 $y \in \{0, 1\}$，累加对数似然函数的导数：

*   **一阶导 (g)**: 衡量了预测概率与实际结果的偏差。
    $$g = \sum \frac{y - P}{\beta}$$
*   **二阶导的相反数 (negH)**: 衡量了信息的确定性（Fisher Information）。
    $$negH = \sum \frac{P(1 - P)}{\beta^2}$$

在贝叶斯框架下，还需要加上先验分布的贡献（这里假设先验是正态分布），此时展开点正好在先验均值上，因此一阶导的先验项为 0，二阶导的先验项为先验精度：

$$negH_{total} = negH + Prec_{prior}$$

**步骤 3: 使用牛顿法更新能力值 ($\theta$)**

计算更新步长，并限制最大单步更新幅度 (`STEP_CAP` = 0.8)，防止因单次成绩波动过大导致评估值跳跃：

$$step = \text{clip}\left(\frac{g}{\max(negH_{total}, 10^{-8})}, -STEP_{CAP}, STEP_{CAP}\right)$$

$$ \theta_{new} = \text{clip}(\theta_{current} + step, THETA_{MIN}, THETA_{MAX}) $$

**步骤 4: 更新方差 (拉普拉斯近似 Laplace Approximation)**

使用更新后的新能力值 $\theta_{new}$ 重新计算观测信息量 $Info$：

$$Info = \sum \frac{P_{new}(1 - P_{new})}{\beta^2}$$

利用拉普拉斯近似，后验方差即为先验精度加观测信息量的倒数：

$$V_{new} = \frac{1}{\max(Prec_{prior} + Info, 10^{-8})}$$

最后，为防止方差过小，设定一个下限（如 `0.05`）。

## 3. 关键参数说明

这些参数目前硬编码在 `AbilityEstimator` 类中：

*   `THETA_MIN = 1.0`, `THETA_MAX = 10.0`: 定义了能力量表的上下界限，也是系统题目难度的度量标准。
*   `BETA = 1.0`: 题目的区分度参数。
*   `PROCESS_NOISE_Q = 0.08`: 过程噪声，允许系统在新材料下继续微调能力值。建议值 `0.05 ~ 0.15`。
*   `STEP_CAP = 0.8`: 单次练习更新能力值的最大跨度。这决定了系统收敛到真实能力的平稳性（一般 20 个材料左右收敛）。

## 4. 优化建议

1.  **参数外部化**: 当前算法中的常量（如 `BETA`, `PROCESS_NOISE_Q`, `STEP_CAP`）是硬编码的。为了便于在不修改代码、不重启服务的情况下进行算法调优（Tuning）和 A/B 测试，建议将这些参数提取到配置文件中。
2.  **多维度能力模型**: 目前模型假设“听音能力”是一个单一的维度 $\theta$。如果业务发展需要，可以考虑将能力拆分为更细致的子维度（如：音高识别、节奏感知、和弦辨听等），并分别维护其 $\theta$ 向量和协方差矩阵（多维 IRT）。
