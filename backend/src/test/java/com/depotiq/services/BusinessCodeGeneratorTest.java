package com.depotiq.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class BusinessCodeGeneratorTest {
    @Mock JdbcTemplate jdbc;

    @Test
    void storeCodesUseThreeDigitsAndSkipLegacyImportedCodes() {
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(12L, 13L, 14L);
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), anyString())).thenReturn(true, true, false);
        assertThat(new BusinessCodeGenerator(jdbc).nextStoreCode()).isEqualTo("S014");
        verify(jdbc).queryForObject(anyString(), eq(Boolean.class), eq("S012"));
        verify(jdbc).queryForObject(anyString(), eq(Boolean.class), eq("S013"));
    }

    @Test
    void productCodesKeepFourDigitsAndSkipOccupiedValues() {
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(101L, 102L);
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), anyString())).thenReturn(true, false);
        assertThat(new BusinessCodeGenerator(jdbc).nextProductCode()).isEqualTo("P0102");
    }

    @Test
    void storeCodesExpandPast999WithoutTruncating() {
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(1000L);
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), anyString())).thenReturn(false);
        assertThat(new BusinessCodeGenerator(jdbc).nextStoreCode()).isEqualTo("S1000");
    }

    @Test
    void legacyPaddingNormalizesWithoutChangingNumericIdentity() {
        assertThat(BusinessCodes.normalizeStoreCode(" S0011 ")).isEqualTo("S011");
        assertThat(BusinessCodes.normalizeStoreCode("s1")).isEqualTo("S001");
        assertThat(BusinessCodes.normalizeStoreCode("S1000")).isEqualTo("S1000");
        assertThat(BusinessCodes.normalizeStoreCode("S0000")).isEqualTo("S000");
        assertThat(BusinessCodes.normalizeStoreCode("POS-NORTH")).isEqualTo("POS-NORTH");
        assertThat(BusinessCodes.compareStoreCodes("S999", "S1000")).isNegative();
    }
}
