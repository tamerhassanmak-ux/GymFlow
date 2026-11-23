package com.mycompany.lazar_app;
import com.mycompany.lazar_app.model.*;
import com.mycompany.lazar_app.model.report.ReportSummary;
import com.mycompany.lazar_app.repository.*;
import com.mycompany.lazar_app.service.*;
import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FinancialRulesTest {
 @Test void RU01_profitMarginUsesDecimalArithmetic(){ReportSummary s=new ReportSummary();s.setTotalRevenue(new BigDecimal("123.45"));s.setTotalExpenses(new BigDecimal("23.45"));assertEquals(new BigDecimal("100.00"),s.getNetProfit());assertEquals(new BigDecimal("81.00"),s.getProfitMargin());}
 @Test void RU02_zeroAndNullRevenueDoNotDivideByZero(){ReportSummary s=new ReportSummary();s.setTotalRevenue(null);s.setTotalExpenses(BigDecimal.TEN);assertEquals(0,s.getProfitMargin().signum());assertEquals(new BigDecimal("-10"),s.getNetProfit());}
 @Test void RU03_reportRangeRejectsMissingAndReversedDates(){ReportService s=new ReportService(new ReportRepository());assertThrows(IllegalArgumentException.class,()->s.getSummary(null,LocalDate.now()));assertThrows(IllegalArgumentException.class,()->s.getSummary(LocalDate.now(),LocalDate.now().minusDays(1)));}
 @Test void RU04_expirationBoundaryAndSessionLimit(){MemberMembership m=new MemberMembership();m.setStartDate(LocalDate.now());m.setEndDate(LocalDate.now());m.setStatus("Active");m.setAllowedSessions(2);assertTrue(m.isActive());assertTrue(m.canCheckIn(1));assertFalse(m.canCheckIn(2));m.setEndDate(LocalDate.now().minusDays(1));assertFalse(m.canCheckIn(0));}
 @Test void RU05_futureSubscriptionCannotCheckIn(){MemberMembership m=new MemberMembership();m.setStartDate(LocalDate.now().plusDays(1));m.setEndDate(LocalDate.now().plusDays(31));m.setStatus("Active");m.setAllowedSessions(12);assertFalse(m.canCheckIn(0));}
 @Test void RU06_subscriptionDiscountUsesCurrencyPrecision(){MemberMembership m=new MemberMembership();m.setPrice(0.3);m.setDiscount(0.2);m.calculateFinalPrice();assertEquals(0.1,m.getFinalPrice());}
 @Test void RU07_nonfiniteDiscountCannotEnterFinancialModel(){MemberMembership m=new MemberMembership();m.setPrice(100);m.setDiscount(Double.NaN);assertThrows(IllegalArgumentException.class,m::calculateFinalPrice);}
 @Test void RU08_javaValidationTranslationKeysExistInBothLocales() throws Exception {
  for(java.util.Locale locale:java.util.List.of(java.util.Locale.ENGLISH,java.util.Locale.forLanguageTag("ar"))){
   var bundle=java.util.ResourceBundle.getBundle("i18n.messages",locale);
   for(String key:java.util.List.of("validation.nameRequired","validation.phoneRequired","validation.statusRequired","checkin.details.sessions","database.auth.title"))assertFalse(bundle.getString(key).isBlank());
  }
 }
 @Test void RU09_configuredCurrencyIsUsedByDisplayHelper(){var settings=com.mycompany.lazar_app.settings.AppSettingsManager.load();String original=settings.getCurrency();try{settings.setCurrency("USD");assertEquals("$",com.mycompany.lazar_app.settings.AppSettingsManager.getCurrencyLabel(false));settings.setCurrency("SAR");assertEquals("SAR",com.mycompany.lazar_app.settings.AppSettingsManager.getCurrencyLabel(false));}finally{settings.setCurrency(original);}}
}
