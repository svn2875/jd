//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://gnu.org/licenses/>.

package jd.plugins.decrypt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.gui.skins.simple.ConvertDialog;
import jd.gui.skins.simple.ConvertDialog.ConversionMode;
import jd.http.Encoding;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

public class YouTubeCom extends PluginForDecrypt {

    // TODO: Neue Plugins im StreamingShare-Addon eintragen!
    static private String host = "youtube.com";
    static private final Pattern patternswfArgs = Pattern.compile("(.*?swfArgs.*)", Pattern.CASE_INSENSITIVE);
    private static final String PLAYER = "get_video";
    private static final String T = "\"t\"";
    private static final String VIDEO_ID = "video_id";
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?youtube\\.com/watch\\?v=[a-z-_A-Z0-9]+|\\< streamingshare=\"youtube\\.com\" name=\".*?\" dlurl=\".*?\" brurl=\".*?\" convertto=\".*?\" comment=\".*?\" \\>", Pattern.CASE_INSENSITIVE);
    private Pattern StreamingShareLink = Pattern.compile("\\< streamingshare=\"youtube\\.com\" name=\"(.*?)\" dlurl=\"(.*?)\" brurl=\"(.*?)\" convertto=\"(.*?)\" comment=\"(.*?)\" \\>", Pattern.CASE_INSENSITIVE);

    static public final Pattern YT_FILENAME = Pattern.compile("<meta name=\"title\" content=\"(.*?)\">", Pattern.CASE_INSENSITIVE);

    public YouTubeCom() {
        super();
        br.setCookiesExclusive(true);
        br.setFollowRedirects(true);
        br.clearCookies("youtube.com");
    }

    private String clean(String s) {
        s = s.replaceAll("\"", "");
        s = s.replaceAll("YouTube -", "");
        s = s.replaceAll("YouTube", "");
        s = s.trim();
        return s;
    }

    private void login(Account account) throws IOException {
        if (account.isEnabled()) {
            br.getPage("http://www.youtube.com/signup?next=/index");
            Form login = br.getFormbyName("loginForm");
            login.put("username", account.getUser());
            login.put("password", account.getPass());
            br.submitForm(login);
            if (!br.getRegex(">YouTube - " + account.getUser() + "'s YouTube<").matches()) {
                logger.severe("Account invalid");
                account.setEnabled(false);
            }
        }
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        Vector<ConversionMode> possibleconverts = new Vector<ConversionMode>();
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        ArrayList<Account> accounts = JDUtilities.getAccountsForHost("youtube.com");
        if (accounts != null) login(accounts.get(0));

        try {
            if (StreamingShareLink.matcher(parameter).matches()) {
                // StreamingShareLink

                String[] info = new Regex(parameter, StreamingShareLink).getMatches()[0];

                for (String debug : info) {
                    logger.info(debug);
                }
                DownloadLink thislink = createDownloadlink(info[1]);
                thislink.setBrowserUrl(info[2]);
                thislink.setStaticFileName(info[0]);
                thislink.setSourcePluginComment("Convert to " + (ConversionMode.valueOf(info[3])).GetText());
                thislink.setProperty("convertto", info[3]);

                decryptedLinks.add(thislink);
                return decryptedLinks;
            }

            br.getPage(parameter);
            if (br.getRegex(Pattern.compile("signup\\?next=/watch")).matches()) { throw new DecrypterException("No Valid Account for Age Check"); }
            Form f = br.getForms()[2];
            if (f != null && f.action == null) br.submitForm(f);
            String video_id = "";
            String t = "";
            String match = br.getRegex(patternswfArgs).getMatch(0);
            if (match == null) { return null; }

            /* DownloadUrl holen */
            String[] lineSub = match.split(",|:");
            for (int i = 0; i < lineSub.length; i++) {
                String s = lineSub[i];
                if (s.indexOf(VIDEO_ID) > -1) {
                    video_id = clean(lineSub[i + 1]);
                }
                if (s.indexOf(T) > -1) {
                    t = clean(lineSub[i + 1]);
                }
            }
            String link = "http://" + host + "/" + PLAYER + "?" + VIDEO_ID + "=" + video_id + "&" + "t=" + t;
            String name = Encoding.htmlDecode(br.getRegex(YT_FILENAME).getMatch(0).trim());
            /* Konvertierungsmöglichkeiten adden */

            if (br.openGetConnection(link + "&fmt=18").getResponseCode() == 200) {
                possibleconverts.add(ConversionMode.VIDEOMP4);
            }
            if (br.openGetConnection(link + "&fmt=13").getResponseCode() == 200) {
                possibleconverts.add(ConversionMode.VIDEO3GP);
            }
            possibleconverts.add(ConversionMode.AUDIOMP3);
            possibleconverts.add(ConversionMode.VIDEOFLV);
            possibleconverts.add(ConversionMode.AUDIOMP3_AND_VIDEOFLV);

            ConversionMode ConvertTo = ConvertDialog.DisplayDialog(possibleconverts.toArray(), name);
            if (ConvertTo != null) {
                if (ConvertTo == ConvertDialog.ConversionMode.VIDEOMP4) {
                    link += "&fmt=18";
                } else if (ConvertTo == ConvertDialog.ConversionMode.VIDEO3GP) {
                    link += "&fmt=13";
                }
                DownloadLink thislink = createDownloadlink(link);
                thislink.setBrowserUrl(parameter);
                thislink.setStaticFileName(name + ".tmp");
                thislink.setSourcePluginComment("Convert to " + ConvertTo.GetText());
                thislink.setProperty("convertto", ConvertTo.name());
                decryptedLinks.add(thislink);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}