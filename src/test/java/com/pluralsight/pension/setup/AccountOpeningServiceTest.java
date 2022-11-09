package com.pluralsight.pension.setup;

import com.pluralsight.pension.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;


import static com.pluralsight.pension.setup.AccountOpeningService.UNACCEPTABLE_RISK_PROFILE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class AccountOpeningServiceTest {

    private static final String FIRST_NAME = "Mohamed";
    private static final String LAST_NAME = "Ahmed";
    private static final String TAX_ID = "100AB";
    private static final LocalDate DOB = LocalDate.of(2000, 8, 14);
    public static final BackgroundCheckResults BACKGROUND_CHECK_RESULTS = new BackgroundCheckResults("Accepted Risk", 50000);
    public static final String VALID_ID = "VALID_ID";
    private AccountOpeningService underTest;
    private BackgroundCheckService backgroundCheckService = mock(BackgroundCheckService.class);
    private ReferenceIdsManager referenceIdsManager = mock(ReferenceIdsManager.class);
    private AccountRepository accountRepository = mock(AccountRepository.class);
    private AccountOpeningEventPublisher accountOpeningEventPublisher = mock(AccountOpeningEventPublisher.class);

    @BeforeEach
    void setUp() {
        underTest = new AccountOpeningService(backgroundCheckService,referenceIdsManager,accountRepository, accountOpeningEventPublisher);
    }

    @Test
    public void shouldOpenAccount() throws IOException {
        BackgroundCheckResults accepted_checks = new BackgroundCheckResults("Accepted Risk", 50000);
        when(backgroundCheckService.confirm(
                FIRST_NAME,
                LAST_NAME,
                TAX_ID,
                DOB
        )).thenReturn(accepted_checks);
        when(referenceIdsManager.obtainId(
                eq(FIRST_NAME),
                anyString(),
                eq(LAST_NAME),
                eq(TAX_ID),
                eq(DOB)
        )).thenReturn(VALID_ID);
        final AccountOpeningStatus accountOpeningStatus = underTest.openAccount(
                FIRST_NAME,
                LAST_NAME,
                TAX_ID,
                DOB
        );
        assertEquals(AccountOpeningStatus.OPENED, accountOpeningStatus);
        verify(accountRepository).save(
                VALID_ID,
                FIRST_NAME,
                LAST_NAME,
                TAX_ID,
                DOB,
                accepted_checks
        );
        verify(accountOpeningEventPublisher).notify(VALID_ID);

    }

    @Test
    public void ShouldDeclineAccountIfBackgroundCheckReturnsUnacceptedProfile() throws IOException {
        when(backgroundCheckService.confirm(
                FIRST_NAME,
                LAST_NAME,
                TAX_ID,
                DOB
        )).thenReturn(new BackgroundCheckResults(UNACCEPTABLE_RISK_PROFILE, 0));
        final AccountOpeningStatus accountOpeningStatus = underTest.openAccount(
                FIRST_NAME,
                LAST_NAME,
                TAX_ID,
                DOB
        );
        assertEquals(AccountOpeningStatus.DECLINED, accountOpeningStatus);
    }

    @Test
    public void ShouldDeclineIfBackgroundCheckReturnsNull() throws IOException {
        when(backgroundCheckService.confirm(
                FIRST_NAME,
                LAST_NAME,
                TAX_ID,
                DOB
        )).thenReturn(null);
        final AccountOpeningStatus accountOpeningStatus = underTest.openAccount(
                FIRST_NAME,
                LAST_NAME,
                TAX_ID,
                DOB
        );
        assertEquals(AccountOpeningStatus.DECLINED, accountOpeningStatus);
    }
    @Test
    public void shouldThrowIfBackgroundChecksServiceThrows() throws IOException {
        when(backgroundCheckService.confirm(
                FIRST_NAME,
                LAST_NAME,
                TAX_ID,
                DOB
        )).thenThrow(new IOException());
        assertThrows(IOException.class, () ->underTest.openAccount(
                FIRST_NAME,
                LAST_NAME,
                TAX_ID,
                DOB
        ));
    }
    @Test
    public void shouldThrowIfReferenceIdsManagerThrows() throws IOException {
        when(backgroundCheckService.confirm(
                FIRST_NAME,
                LAST_NAME,
                TAX_ID,
                DOB
        )).thenReturn(new BackgroundCheckResults("Accepted Risk", 50000));
        when(referenceIdsManager.obtainId(
                eq(FIRST_NAME),
                anyString(),
                eq(LAST_NAME),
                eq(TAX_ID),
                eq(DOB)
        )).thenThrow(new RuntimeException());
        assertThrows(RuntimeException.class, () ->underTest.openAccount(
                FIRST_NAME,
                LAST_NAME,
                TAX_ID,
                DOB
        ));
    }
    @Test
    public void shouldThrowIfAccountRepositoryThrows() throws IOException {
        when(backgroundCheckService.confirm(
                FIRST_NAME,
                LAST_NAME,
                TAX_ID,
                DOB
        )).thenReturn(BACKGROUND_CHECK_RESULTS);
        when(referenceIdsManager.obtainId(
                eq(FIRST_NAME),
                anyString(),
                eq(LAST_NAME),
                eq(TAX_ID),
                eq(DOB)
        )).thenReturn("validId");
        when(accountRepository.save(
                "validId",
                FIRST_NAME,
                LAST_NAME,
                TAX_ID,
                DOB,
                BACKGROUND_CHECK_RESULTS
        )).thenThrow(new RuntimeException());
        assertThrows(RuntimeException.class, () ->underTest.openAccount(
                FIRST_NAME,
                LAST_NAME,
                TAX_ID,
                DOB
        ));
    }
    @Test
    public void shouldThrowIfEventPublisherThrows() throws IOException {
        when(backgroundCheckService.confirm(
                FIRST_NAME,
                LAST_NAME,
                TAX_ID,
                DOB
        )).thenReturn(BACKGROUND_CHECK_RESULTS);
        when(referenceIdsManager.obtainId(
                eq(FIRST_NAME),
                anyString(),
                eq(LAST_NAME),
                eq(TAX_ID),
                eq(DOB)
        )).thenReturn("validId");
        when(accountRepository.save(
                "validId",
                FIRST_NAME,
                LAST_NAME,
                TAX_ID,
                DOB,
                BACKGROUND_CHECK_RESULTS
        )).thenReturn(true);
        doThrow(new RuntimeException()).when(accountOpeningEventPublisher).notify("validId");
        assertThrows(RuntimeException.class, () ->underTest.openAccount(
                FIRST_NAME,
                LAST_NAME,
                TAX_ID,
                DOB
        ));
    }
}