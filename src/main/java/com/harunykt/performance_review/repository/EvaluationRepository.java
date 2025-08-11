package com.harunykt.performance_review.repository;

import com.harunykt.performance_review.model.Evaluation;
import com.harunykt.performance_review.model.PeriodQuarter;
import com.harunykt.performance_review.model.User;
import com.harunykt.performance_review.model.EvaluationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

 // Belirli bir kullanıcı hakkında yapılan değerlendirmeler
 List<Evaluation> findByEvaluated(User evaluated);

 // Belirli bir kullanıcı hakkında yapılan belirli türde değerlendirmeler
 List<Evaluation> findByEvaluatedAndType(User evaluated, EvaluationType type);

 // Belirli bir kişinin yaptığı tüm değerlendirmeler
 List<Evaluation> findByEvaluator(User evaluator);

 // Belirli türde yapılan tüm değerlendirmeler (örneğin: MANAGER_TO_EMPLOYEE)
 List<Evaluation> findByType(EvaluationType type);


 Optional <Evaluation> findFirstByEvaluatorIdAndEvaluatedIdAndTypeAndPeriodYearAndPeriodQuarter(
         Long evaluatorId, Long evaluatedId , EvaluationType type, Integer periodYear, PeriodQuarter periodQuarter
 );


}
