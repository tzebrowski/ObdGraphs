package org.obd.graphs.profile

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses

@RunWith(Suite::class)
@SuiteClasses(
    LoadProfilesTest::class,
    BackupTest::class,
    ProfileServiceTest::class,
    SetupProfilesTest::class
)
class ProfileTestSuite {
}