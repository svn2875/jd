//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
//
//    Este plugin ha sido modificado por el_joker333 <el_joker333@hotmail.com>
//


package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.pluginUtils.Recaptcha;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hotfile.com" }, urls = { "http://[\\w\\.]*?hotfile\\.com/dl/\\d+/[0-9a-zA-Z]+/(.*?/|.+)" }, flags = { 2 })
public class HotFileCom extends PluginForHost {

    private boolean skipperFailed = false;

    public HotFileCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://hotfile.com/register.html?reff=1510459");
        setConfigElements();
    }

    private static final String UNLIMITEDMAXCON = "UNLIMITEDMAXCON";

    private void setConfigElements() {
        ConfigEntry cond = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), UNLIMITEDMAXCON, JDL.L("plugins.hoster.HotFileCom.SetUnlimitedConnectionsForPremium", "Allow more than 5 connections per file for premium (default maximum = 5). Enabling this can cause errors!!")).setDefaultValue(false);
        config.addEntry(cond);
    }

    @Override
    public String getAGBLink() {
        return "http://hotfile.com/terms-of-service.html";
    }

    public void login(Account account) throws IOException, PluginException {
        if (account.getUser().trim().equalsIgnoreCase("cookie")) {
            this.setBrowserExclusive();
            br.setCookie("http://hotfile.com", "lang", "en");
            br.setCookie("http://hotfile.com", "auth", account.getPass());
            br.getPage("http://hotfile.com/");
            if (br.getCookie("http://hotfile.com/", "auth") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
				    br.getPage("http://hotfile.com/premiuminfo.html?lang=en");
				    if (!br.containsHTML(">Premium Membership<")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
				    br.setFollowRedirects(false);
        }
        else{
				    this.setBrowserExclusive();
				    br.setDebug(true);
				    br.setCookie("http://hotfile.com", "lang", "en");
				    br.setFollowRedirects(true);
				    br.getPage("http://hotfile.com/");
				    br.postPage("http://hotfile.com/login.php", "returnto=%2F&user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
				    Form form = br.getForm(0);
				    if (form != null && form.containsHTML("<td>Username:")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
				    if (br.getCookie("http://hotfile.com/", "auth") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
				    br.getPage("http://hotfile.com/premiuminfo.html?lang=en");
				    if (!br.containsHTML(">Premium Membership<")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
				    br.setFollowRedirects(false);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        String validUntil = br.getRegex("<td>Until.*?</td><td>(.*?)</td>").getMatch(0);
        if (validUntil == null) {
            account.setValid(false);
        } else {
            ai.setValidUntil(Regex.getMilliSeconds(validUntil, "yyyy-MM-dd HH:mm:ss", null));
            account.setValid(true);
        }
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        String finalUrl = null;
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            finalUrl = br.getRedirectLocation();
        } else {
            finalUrl = br.getRegex("<h3 style='margin-top: 20px'><a href=\"(.*?hotfile.*?)\">Click here to download</a></h3>").getMatch(0);
            if (finalUrl == null) finalUrl = br.getRegex("table id=\"download_file\".*?<a href=\"(.*?)\"").getMatch(0);/* polish */
        }
        br.setFollowRedirects(true);
        if (finalUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Set the meximum connections per file
        boolean waitReconnecttime = getPluginConfig().getBooleanProperty(UNLIMITEDMAXCON, false);
        int maxcon = -5;
        if (waitReconnecttime) maxcon = 0;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finalUrl, true, maxcon);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            finalUrl = br.getRegex("<h3 style='margin-top: 20px'><a href=\"(.*?hotfile.*?)\">Click here to download</a></h3>").getMatch(0);
            if (finalUrl == null) finalUrl = br.getRegex("table id=\"download_file\".*?<a href=\"(.*?)\"").getMatch(0);/* polish */
            if (finalUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finalUrl, true, maxcon);
        }
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* filename workaround */
        String urlFileName = Plugin.getFileNameFromURL(new URL(br.getURL()));
        urlFileName = Encoding.htmlDecode(urlFileName);
        downloadLink.setFinalFileName(urlFileName);
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        /* workaround as server does not send correct encoding information */
        br.setCustomCharset("UTF-8");
        br.setCookie("http://hotfile.com", "lang", "en");
        br.getPage(parameter.getDownloadURL());
        if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
        String filename = br.getRegex("Downloading <b>(.+?)</b>").getMatch(0);
        if (filename == null) {
            /* polish users get this */
            filename = br.getRegex("Downloading:</strong>(.*?)<").getMatch(0);
        }
        String filesize = br.getRegex("<span class=\"size\">(.*?)</span>").getMatch(0);
        if (filesize == null) {
            /* polish users get this */
            filesize = br.getRegex("Downloading:</strong>.*?span.*?strong>(.*?)<").getMatch(0);
        }
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.setDebug(true);
        if (br.containsHTML("You are currently downloading")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
        if (br.containsHTML("starthtimer\\(\\)")) {
            String waittime = br.getRegex("starthtimer\\(\\).*?timerend=.*?\\+(\\d+);").getMatch(0);
            if (Long.parseLong(waittime.trim()) > 0) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(waittime.trim())); }
        }
        Form[] forms = br.getForms();
        Form form = forms[1];
        long sleeptime = 0;
        try {
            sleeptime = Long.parseLong(br.getRegex("timerend=d\\.getTime\\(\\)\\+(\\d+);").getMatch(0)) + 1;
            // for debugging purposes
            logger.info("Regexed waittime is " + sleeptime + " seconds");
        } catch (Exception e) {
            logger.info("WaittimeRegex broken");
            logger.info(br.toString());
            sleeptime = 60 * 1000l;
        }
        // Reconnect if the waittime is too big!
        if (sleeptime > 100 * 1000l) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, sleeptime);
        // try to skip waittime, if this fails, fallback to waittime
        if (!this.skipperFailed) {
            form.put("tm", "1245072880");
            form.put("tmhash", "e5b845119f0055c5d8554ee5f2ffc7b2d5ef86d7");
            form.put("wait", "30");
            form.put("waithash", "3bf07c5d83f2e652ff22eeaee00a6f08d4d2409a");
            br.submitForm(form);
            if (br.containsHTML("name=wait") && !this.skipperFailed) {
                skipperFailed = true;
                handleFree(link);
                return;
            }
        } else {
            this.sleep(sleeptime, link);
            br.submitForm(form);
        }
        // captcha
        if (!br.containsHTML("Click here to download")) {
            Recaptcha rc = new Recaptcha(br);
            rc.parse();
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, link);
            rc.setCode(c);
            if (!br.containsHTML("Click here to download")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String dl_url = br.getRegex("<h3 style='margin-top: 20px'><a href=\"(.*?)\">Click here to download</a>").getMatch(0);
        if (dl_url == null) dl_url = br.getRegex("table id=\"download_file\".*?<a href=\"(.*?)\"").getMatch(0);/* polish */
        if (dl_url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        br.setDebug(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dl_url, false, 1);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("You are currently downloading")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* filename workaround */
        String urlFileName = Plugin.getFileNameFromURL(new URL(br.getURL()));
        urlFileName = Encoding.htmlDecode(urlFileName);
        link.setFinalFileName(urlFileName);
        dl.startDownload();
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("pl.hotfile.com", "hotfile.com"));
    }

    @Override
    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            Browser br = new Browser();
            br.setCookiesExclusive(true);
            br.setCookie("http://hotfile.com", "lang", "en");
            StringBuilder sb = new StringBuilder();
            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                br.getPage("http://hotfile.com/checkfiles.html");
                links.clear();
                while (true) {
                    if (index == urls.length || links.size() > 25) break;
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                sb.append("files=");
                int c = 0;
                for (DownloadLink dl : links) {
                    /*
                     * append fake filename, because api will not report
                     * anything else
                     */
                    if (c > 0) sb.append("%0D%0A");
                    sb.append(Encoding.urlEncode(dl.getDownloadURL() + "filecheck.html"));
                    c++;
                }
                sb.append("&but=+Check+Urls+");
                br.postPage("http://hotfile.com/checkfiles.html", sb.toString());
                for (DownloadLink dl : links) {
                    String id = new Regex(dl.getDownloadURL(), "hotfile\\.com(/dl/\\d+/[0-9a-zA-Z]+/)").getMatch(0);

                    String[] dat = br.getRegex("<b>Results</b>.*<tr>.*?<td>(.*?" + id + ".*?)</td>.*?<td>(.*?)</td>.*?<td.*?>.*?<span.*?>(.*?)</span>.*?</td>.*?</tr>").getRow(0);
                    if (dat != null) {
                        String name = dat[0].trim();
                        String size = dat[1].trim();
                        String status = dat[2].trim();

                        if (name.startsWith("<a href")) {
                            name = new Regex(name, "hotfile.com.*\\/(.*?)\\<").getMatch(0);
                        } else {
                            name = name.substring(name.lastIndexOf("/") + 1);
                        }

                        if (name != null && size != null && status.equalsIgnoreCase("Existent")) {
                            name = name.replaceAll("\\.html", "").trim();
                            dl.setName(name);
                            dl.setDownloadSize(Regex.getSize(size.trim()));
                            dl.setAvailable(true);
                        } else {
                            dl.setAvailable(false);
                        }
                    } else {
                        dl.setAvailable(false);
                    }
                }
                if (index == urls.length) break;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
