// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.openqa.selenium.remote.server;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;
import static org.openqa.selenium.remote.server.CapabilitiesComparator.getBestMatch;

import com.google.common.collect.Lists;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Platform;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.util.Comparator;
import java.util.List;

@RunWith(JUnit4.class)
public class CapabilitiesComparatorTest {

  private Comparator<Capabilities> comparator;

  @Test
  public void shouldMatchByBrowserName_assumingAllOtherPropertiesAreNull() {
    comparator = compareBy(capabilities(BrowserType.FIREFOX, "", Platform.ANY, true));

    Capabilities c1 = capabilities(BrowserType.FIREFOX, null, null, false);
    Capabilities c2 = capabilities(BrowserType.CHROME, null, null, false);

    assertGreaterThan(c1, c2);
  }

  @Test
  public void shouldMatchByBrowserName_assumingAllOtherPropertiesAreTheSame() {
    comparator = compareBy(capabilities(BrowserType.FIREFOX, "", Platform.ANY, true));

    Capabilities c1 = capabilities(BrowserType.FIREFOX, "", Platform.ANY, true);
    Capabilities c2 = capabilities(BrowserType.CHROME, "", Platform.ANY, true);

    assertGreaterThan(c1, c2);
  }

  @Test
  public void shouldIgnoreVersionIfNullOnAnInput() {
    comparator = compareBy(capabilities(BrowserType.FIREFOX, "6", Platform.ANY, true));

    Capabilities c1 = capabilities(BrowserType.FIREFOX, null, Platform.ANY, true);
    Capabilities c2 = capabilities(BrowserType.FIREFOX, "7", Platform.ANY, true);
    assertGreaterThan(c1, c2);

    Capabilities c3 = capabilities(BrowserType.FIREFOX, "7", Platform.ANY, true);
    Capabilities c4 = capabilities(BrowserType.FIREFOX, null, Platform.ANY, true);
    assertGreaterThan(c4, c3);
  }

  @Test
  public void shouldMatchByVersion_assumingAllOtherPropertiesAreTheSame_versionSpecified() {
    comparator = compareBy(capabilities(BrowserType.FIREFOX, "6", Platform.ANY, true));

    Capabilities c1 = capabilities(BrowserType.FIREFOX, "6", Platform.ANY, true);
    Capabilities c2 = capabilities(BrowserType.FIREFOX, "7", Platform.ANY, true);
    Capabilities c3 = capabilities(BrowserType.FIREFOX, null, Platform.ANY, true);

    assertGreaterThan(c1, c2);
    assertGreaterThan(c1, c3);
  }

  @Test
  public void shouldMatchByPlatform_assumingAllOtherPropertiesAreTheSame() {
    comparator = compareBy(capabilities(BrowserType.FIREFOX, "6", Platform.ANY, true));

    Capabilities c1 = capabilities(BrowserType.FIREFOX, "6", Platform.ANY, true);
    Capabilities c2 = capabilities(BrowserType.FIREFOX, "6", Platform.LINUX, true);

    assertGreaterThan(c1, c2);
  }

  @Test
  public void shouldPreferCurrentPlatformOverOthers() {
    comparator = compareBy(capabilities(BrowserType.FIREFOX, "6", Platform.ANY, true), Platform.LINUX);

    Capabilities c1 = capabilities(BrowserType.FIREFOX, "6", Platform.ANY, true);
    Capabilities c2 = capabilities(BrowserType.FIREFOX, "6", Platform.LINUX, true);
    Capabilities c3 = capabilities(BrowserType.FIREFOX, "6", Platform.WINDOWS, true);

    assertGreaterThan(c2, c1);
    assertGreaterThan(c2, c3);
  }

  @Test
  public void shouldPickCorrectBrowser() {
    Capabilities chrome = DesiredCapabilities.chrome();
    Capabilities firefox = DesiredCapabilities.firefox();
    Capabilities opera = DesiredCapabilities.opera();
    List<Capabilities> list = Lists.newArrayList(chrome, firefox, opera);

    DesiredCapabilities desired = new DesiredCapabilities();

    desired.setBrowserName(BrowserType.CHROME);
    assertThat(getBestMatch(desired, list), equalTo(chrome));

    desired.setBrowserName(BrowserType.FIREFOX);
    assertThat(getBestMatch(desired, list), equalTo(firefox));

    desired.setBrowserName(BrowserType.OPERA);
    assertThat(getBestMatch(desired, list), equalTo(opera));
  }

  @Test
  public void shouldPickAnyIfPlatformChoicesAreAnyOrWindowsAndDesireLinux() {
    Capabilities any = capabilities(BrowserType.FIREFOX, "", Platform.ANY, true);
    Capabilities windows = capabilities(BrowserType.FIREFOX, "", Platform.WINDOWS, true);
    Capabilities linux = capabilities(BrowserType.FIREFOX, "", Platform.LINUX, true);

    assertThat(getBestMatch(linux, Lists.newArrayList(any, windows)), equalTo(any));
    // Registration order should not matter.
    assertThat(getBestMatch(linux, Lists.newArrayList(windows, any)), equalTo(any));
  }

  @Test
  public void shouldPickWindowsIfPlatformChoiceIsAny() {
    Capabilities any = capabilities(BrowserType.IE, "", Platform.ANY, true);
    Capabilities windows = capabilities(BrowserType.IE, "", Platform.WINDOWS, true);
    assertThat(getBestMatch(any, Lists.newArrayList(windows)), equalTo(windows));
  }

  @Test
  public void shouldPickMostSpecificOperatingSystem() {
    Capabilities any = capabilities(BrowserType.IE, "", Platform.ANY, true);
    Capabilities windows = capabilities(BrowserType.IE, "", Platform.WINDOWS, true);
    Capabilities xp = capabilities(BrowserType.IE, "", Platform.XP, true);
    Capabilities vista = capabilities(BrowserType.IE, "", Platform.VISTA, true);

    List<Capabilities> list = newArrayList(any, windows, xp, vista);
    assertThat(getBestMatch(any, list), equalTo(any));
    assertThat(getBestMatch(windows, list), equalTo(windows));
    assertThat(getBestMatch(xp, list), equalTo(xp));
    assertThat(getBestMatch(vista, list), equalTo(vista));
  }

  @Test
  public void pickingWindowsFromVariousLists() {
    Capabilities any = capabilities(BrowserType.IE, "", Platform.ANY, true);
    Capabilities windows = capabilities(BrowserType.IE, "", Platform.WINDOWS, true);
    Capabilities xp = capabilities(BrowserType.IE, "", Platform.XP, true);
    Capabilities vista = capabilities(BrowserType.IE, "", Platform.VISTA, true);

    assertThat(getBestMatch(windows, newArrayList(any)), equalTo(any));
    assertThat(getBestMatch(windows, newArrayList(any, windows)), equalTo(windows));
    assertThat(getBestMatch(windows, newArrayList(windows, xp, vista)), equalTo(windows));
    assertThat(getBestMatch(windows, newArrayList(xp, vista)),
        anyOf(equalTo(xp), equalTo(vista)));
    assertThat(getBestMatch(windows, newArrayList(xp)), equalTo(xp));
    assertThat(getBestMatch(windows, newArrayList(vista)), equalTo(vista));
  }

  @Test
  public void pickingXpFromVariousLists() {
    Capabilities any = capabilities(BrowserType.IE, "", Platform.ANY, true);
    Capabilities windows = capabilities(BrowserType.IE, "", Platform.WINDOWS, true);
    Capabilities xp = capabilities(BrowserType.IE, "", Platform.XP, true);
    Capabilities vista = capabilities(BrowserType.IE, "", Platform.VISTA, true);

    assertThat(getBestMatch(xp, newArrayList(any)), equalTo(any));
    assertThat(getBestMatch(xp, newArrayList(any, windows)), equalTo(windows));
    assertThat(getBestMatch(xp, newArrayList(windows, xp, vista)), equalTo(xp));
    assertThat(getBestMatch(xp, newArrayList(windows, xp)), equalTo(xp));
    assertThat(getBestMatch(xp, newArrayList(xp, vista)), equalTo(xp));
    assertThat(getBestMatch(xp, newArrayList(xp)), equalTo(xp));
    assertThat(getBestMatch(xp, newArrayList(vista)), equalTo(vista));
  }

  @Test
  public void pickingVistaFromVariousLists() {
    Capabilities any = capabilities(BrowserType.IE, "", Platform.ANY, true);
    Capabilities windows = capabilities(BrowserType.IE, "", Platform.WINDOWS, true);
    Capabilities xp = capabilities(BrowserType.IE, "", Platform.XP, true);
    Capabilities vista = capabilities(BrowserType.IE, "", Platform.VISTA, true);

    Platform current = Platform.WINDOWS;
    assertThat(getBestMatch(vista, newArrayList(any), current), equalTo(any));
    assertThat(getBestMatch(vista, newArrayList(any, windows), current), equalTo(windows));
    assertThat(getBestMatch(vista, newArrayList(windows, xp, vista), current), equalTo(vista));
    assertThat(getBestMatch(vista, newArrayList(windows, xp), current), equalTo(windows));
    assertThat(getBestMatch(vista, newArrayList(xp, vista), current), equalTo(vista));
    assertThat(getBestMatch(vista, newArrayList(xp), current), equalTo(xp));
    assertThat(getBestMatch(vista, newArrayList(vista), current), equalTo(vista));

    current = Platform.VISTA;
    assertThat(getBestMatch(vista, newArrayList(any), current), equalTo(any));
    assertThat(getBestMatch(vista, newArrayList(any, windows), current), equalTo(windows));
    assertThat(getBestMatch(vista, newArrayList(any, vista), current), equalTo(vista));
    assertThat(getBestMatch(vista, newArrayList(windows, xp, vista), current), equalTo(vista));
    assertThat(getBestMatch(vista, newArrayList(windows, xp), current), equalTo(windows));
    assertThat(getBestMatch(vista, newArrayList(xp, vista), current), equalTo(vista));
    assertThat(getBestMatch(vista, newArrayList(xp), current), equalTo(xp));
    assertThat(getBestMatch(vista, newArrayList(vista), current), equalTo(vista));

    current = Platform.XP;
    assertThat(getBestMatch(vista, newArrayList(any), current), equalTo(any));
    assertThat(getBestMatch(vista, newArrayList(any, windows), current), equalTo(windows));
    assertThat(getBestMatch(vista, newArrayList(any, vista), current), equalTo(vista));
    assertThat(getBestMatch(vista, newArrayList(windows, xp, vista), current), equalTo(vista));
    assertThat(getBestMatch(vista, newArrayList(windows, xp), current), equalTo(windows));
    assertThat(getBestMatch(vista, newArrayList(xp, vista), current), equalTo(vista));
    assertThat(getBestMatch(vista, newArrayList(xp), current), equalTo(xp));
    assertThat(getBestMatch(vista, newArrayList(vista), current), equalTo(vista));
  }

  @Test
  public void pickingUnixFromVariousLists() {
    Capabilities any = capabilities(BrowserType.FIREFOX, "", Platform.ANY, true);
    Capabilities mac = capabilities(BrowserType.FIREFOX, "", Platform.MAC, true);
    Capabilities unix = capabilities(BrowserType.FIREFOX, "", Platform.UNIX, true);
    Capabilities linux = capabilities(BrowserType.FIREFOX, "", Platform.LINUX, true);

    assertThat(getBestMatch(unix, newArrayList(any)), equalTo(any));
    assertThat(getBestMatch(unix, newArrayList(any, mac)), equalTo(any));
    assertThat(getBestMatch(unix, newArrayList(any, unix)), equalTo(unix));
    assertThat(getBestMatch(unix, newArrayList(any, unix, linux)), equalTo(unix));
    assertThat(getBestMatch(unix, newArrayList(unix, linux)), equalTo(unix));
    assertThat(getBestMatch(unix, newArrayList(linux)), equalTo(linux));
  }

  @Test
  public void pickingLinuxFromVariousLists() {
    Capabilities any = capabilities(BrowserType.FIREFOX, "", Platform.ANY, true);
    Capabilities mac = capabilities(BrowserType.FIREFOX, "", Platform.MAC, true);
    Capabilities unix = capabilities(BrowserType.FIREFOX, "", Platform.UNIX, true);
    Capabilities linux = capabilities(BrowserType.FIREFOX, "", Platform.LINUX, true);

    assertThat(getBestMatch(linux, newArrayList(any)), equalTo(any));
    assertThat(getBestMatch(linux, newArrayList(any, mac)), equalTo(any));
    assertThat(getBestMatch(linux, newArrayList(any, unix)), equalTo(unix));
    assertThat(getBestMatch(linux, newArrayList(any, unix, linux)), equalTo(linux));
    assertThat(getBestMatch(linux, newArrayList(unix, linux)), equalTo(linux));
    assertThat(getBestMatch(linux, newArrayList(linux)), equalTo(linux));
    assertThat(getBestMatch(linux, newArrayList(unix)), equalTo(unix));
  }

  @Test
  public void matchesByCapabilitiesProvided() {
    DesiredCapabilities sparse = new DesiredCapabilities();
    sparse.setBrowserName(BrowserType.FIREFOX);

    Capabilities windows = capabilities(BrowserType.IE, "", Platform.WINDOWS, true);
    Capabilities firefox = capabilities(BrowserType.FIREFOX, "", Platform.WINDOWS, true);

    assertThat(getBestMatch(sparse, Lists.newArrayList(windows, firefox)),
        equalTo(firefox));

    sparse.setBrowserName(BrowserType.IE);
    assertThat(getBestMatch(sparse, Lists.newArrayList(windows, firefox)),
        equalTo(windows));
  }

  @Test
  public void matchesWithPreferenceToCurrentPlatform() {
    Capabilities chromeUnix = capabilities(BrowserType.CHROME, "", Platform.UNIX, true);
    Capabilities chromeVista = capabilities(BrowserType.CHROME, "", Platform.VISTA, true);
    Capabilities anyChrome = DesiredCapabilities.chrome();

    List<Capabilities> allCaps = newArrayList(anyChrome, chromeVista, chromeUnix,
        // This last option should never match.
        DesiredCapabilities.firefox());

    // Should match to corresponding platform.
    assertThat(getBestMatch(anyChrome, allCaps, Platform.UNIX), equalTo(chromeUnix));
    assertThat(getBestMatch(chromeUnix, allCaps, Platform.UNIX), equalTo(chromeUnix));

    assertThat(getBestMatch(anyChrome, allCaps, Platform.LINUX), equalTo(chromeUnix));
    assertThat(getBestMatch(chromeUnix, allCaps, Platform.LINUX), equalTo(chromeUnix));

    assertThat(getBestMatch(anyChrome, allCaps, Platform.VISTA), equalTo(chromeVista));
    assertThat(getBestMatch(chromeVista, allCaps, Platform.VISTA), equalTo(chromeVista));

    assertThat(getBestMatch(anyChrome, allCaps, Platform.WINDOWS), equalTo(chromeVista));
    assertThat(getBestMatch(chromeVista, allCaps, Platform.WINDOWS), equalTo(chromeVista));

    // No configs registered to current platform, should fallback to normal matching rules.
    assertThat(getBestMatch(anyChrome, allCaps, Platform.MAC), equalTo(anyChrome));
    assertThat(getBestMatch(anyChrome, allCaps, Platform.XP), equalTo(anyChrome));
  }

  @Test
  public void currentPlatformCheckDoesNotTrumpExactPlatformMatch() {
    Capabilities chromeUnix = capabilities(BrowserType.CHROME, "", Platform.UNIX, true);
    Capabilities chromeVista = capabilities(BrowserType.CHROME, "", Platform.VISTA, true);
    Capabilities anyChrome = DesiredCapabilities.chrome();

    List<Capabilities> allCaps = newArrayList(anyChrome, chromeVista, chromeUnix);

    assertThat(getBestMatch(chromeVista, allCaps, Platform.UNIX), equalTo(chromeVista));
    assertThat(getBestMatch(chromeVista, allCaps, Platform.LINUX), equalTo(chromeVista));
    assertThat(getBestMatch(chromeVista, allCaps, Platform.MAC), equalTo(chromeVista));

    assertThat(getBestMatch(chromeUnix, allCaps, Platform.MAC), equalTo(chromeUnix));
    assertThat(getBestMatch(chromeUnix, allCaps, Platform.VISTA), equalTo(chromeUnix));
    assertThat(getBestMatch(chromeUnix, allCaps, Platform.WINDOWS), equalTo(chromeUnix));
  }

  @Test
  public void currentPlatformCheckDoesNotTrumpExactVersionMatch() {
    Capabilities chromeUnix = capabilities(BrowserType.CHROME, "", Platform.UNIX, true);
    Capabilities chromeBetaUnix = capabilities(BrowserType.CHROME, "beta", Platform.UNIX, true);
    Capabilities chromeVista = capabilities(BrowserType.CHROME, "", Platform.VISTA, true);
    Capabilities anyChrome = DesiredCapabilities.chrome();

    List<Capabilities> allCaps = newArrayList(anyChrome, chromeVista, chromeUnix, chromeBetaUnix);

    assertThat(getBestMatch(chromeUnix, allCaps, Platform.UNIX), equalTo(chromeUnix));
    assertThat(getBestMatch(chromeBetaUnix, allCaps, Platform.UNIX), equalTo(chromeBetaUnix));
  }

  @Test
  public void absentExactMatchPrefersItemsInInputOrder() {
    Capabilities chromeWindows = capabilities(BrowserType.CHROME, "", Platform.WINDOWS, true);
    Capabilities chromeVista = capabilities(BrowserType.CHROME, "", Platform.VISTA, true);
    Capabilities anyChrome = DesiredCapabilities.chrome();

    List<Capabilities> allCaps = newArrayList(chromeWindows, chromeVista);
    List<Capabilities> reversedCaps = Lists.reverse(allCaps);

    assertThat(getBestMatch(anyChrome, allCaps, Platform.UNIX), equalTo(chromeWindows));
    assertThat(getBestMatch(anyChrome, reversedCaps, Platform.UNIX), equalTo(chromeVista));
  }

  @Test
  public void filtersByVersionStringIfNonEmpty() {
    Capabilities anyChrome = DesiredCapabilities.chrome();
    Capabilities chromeBeta = new DesiredCapabilities(anyChrome) {{ setVersion("beta"); }};
    Capabilities chromeDev = new DesiredCapabilities(anyChrome) {{ setVersion("dev"); }};

    List<Capabilities> allCaps = newArrayList(anyChrome, chromeBeta, chromeDev);

    assertThat(getBestMatch(anyChrome, allCaps), equalTo(anyChrome));
    assertThat(getBestMatch(chromeBeta, allCaps), equalTo(chromeBeta));
    assertThat(getBestMatch(chromeDev, allCaps), equalTo(chromeDev));
  }

  @Test
  public void ignoresVersionStringIfEmpty() {
    Capabilities anyChrome = DesiredCapabilities.chrome();
    Capabilities chromeNoVersion = new DesiredCapabilities() {{
      setBrowserName(BrowserType.CHROME);
      setPlatform(Platform.UNIX);
    }};
    Capabilities chromeEmptyVersion = new DesiredCapabilities(chromeNoVersion) {{
      setVersion("");
    }};

    List<Capabilities> allCaps = newArrayList(anyChrome, chromeNoVersion);

    assertThat(getBestMatch(chromeEmptyVersion, allCaps, Platform.UNIX), equalTo(chromeNoVersion));
    assertThat(getBestMatch(chromeNoVersion, allCaps, Platform.UNIX), equalTo(chromeNoVersion));
    // Unix does not match windows.
    assertThat(getBestMatch(anyChrome, allCaps, Platform.WINDOWS), equalTo(anyChrome));
  }

  private void assertGreaterThan(Capabilities a, Capabilities b) {
    assertThat(comparator.compare(a, b), greaterThan(0));
    assertThat(comparator.compare(b, a), lessThan(0));
  }

  private static Comparator<Capabilities> compareBy(Capabilities capabilities) {
    return compareBy(capabilities, Platform.ANY);
  }

  private static Comparator<Capabilities> compareBy(Capabilities capabilities,
      Platform currentPlatform) {
    return new CapabilitiesComparator(capabilities, currentPlatform);
  }

  private static Capabilities capabilities(String browserName, String version,
      Platform platform, boolean isJavaScriptEnabled) {
    DesiredCapabilities dc = new DesiredCapabilities(browserName, version, platform);
    dc.setJavascriptEnabled(isJavaScriptEnabled);
    return dc;
  }

}
