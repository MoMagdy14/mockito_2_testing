package com.pluralsight.pension.setup;

import com.pluralsight.pension.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;


import static com.pluralsight.pension.setup.AccountOpeningService.UNACCEPTABLE_RISK_PROFILE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountOpeningServiceTest {

    private static final String FIRST_NAME = "Mohamed";
    private static final String LAST_NAME = "Ahmed";
    private static final String TAX_ID = "100AB";
    private static final LocalDate DOB = LocalDate.of(2000, 8, 14);
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
        when(backgroundCheckService.confirm(
                FIRST_NAME,
                LAST_NAME,
                TAX_ID,
                DOB
        )).thenReturn(new BackgroundCheckResults("Accepted Risk", 50000));
        when(referenceIdsManager.obtainId(
                FIRST_NAME,
                LAST_NAME,
                TAX_ID,
                DOB
        )).thenReturn("VALID_ID");
        final AccountOpeningStatus accountOpeningStatus = underTest.openAccount(
                FIRST_NAME,
                LAST_NAME,
                TAX_ID,
                DOB
        );
        assertEquals(AccountOpeningStatus.OPENED, accountOpeningStatus);

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
}