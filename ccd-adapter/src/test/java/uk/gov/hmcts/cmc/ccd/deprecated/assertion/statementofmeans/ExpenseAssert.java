package uk.gov.hmcts.cmc.ccd.deprecated.assertion.statementofmeans;

import org.assertj.core.api.AbstractAssert;
import uk.gov.hmcts.cmc.ccd.deprecated.domain.statementofmeans.CCDExpense;
import uk.gov.hmcts.cmc.domain.models.statementofmeans.Expense;

import java.util.Objects;

public class ExpenseAssert extends AbstractAssert<ExpenseAssert, Expense> {

    public ExpenseAssert(Expense actual) {
        super(actual, ExpenseAssert.class);
    }

    public ExpenseAssert isEqualTo(CCDExpense ccdExpense) {
        isNotNull();

        if (!Objects.equals(actual.getType(), ccdExpense.getType())) {
            failWithMessage("Expected Expense.type to be <%s> but was <%s>",
                ccdExpense.getType(), actual.getType());
        }

        if (!Objects.equals(actual.getFrequency().name(), ccdExpense.getFrequency().name())) {
            failWithMessage("Expected Expense.frequency to be <%s> but was <%s>",
                ccdExpense.getFrequency().name(), actual.getFrequency().name());
        }

        if (!Objects.equals(actual.getAmount(), ccdExpense.getAmountPaid())) {
            failWithMessage("Expected Expense.amount to be <%s> but was <%s>",
                ccdExpense.getAmountPaid(), actual.getAmount());
        }

        if (!Objects.equals(actual.getOtherName().orElse(null), ccdExpense.getOtherName())) {
            failWithMessage("Expected Expense.otherName to be <%s> but was <%s>",
                ccdExpense.getOtherName(), actual.getOtherName());
        }
        return this;
    }
}