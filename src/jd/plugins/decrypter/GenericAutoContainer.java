package jd.plugins.decrypter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.LinkCrawlerConfig;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "genericautocontainer" }, urls = { "https?://[\\w\\.:\\-@]*/.*\\.(dlc|ccf|rsdf|nzb)$" }) public class GenericAutoContainer extends PluginForDecrypt {

    @Override
    public Boolean siteTesterDisabled() {
        return Boolean.TRUE;
    }

    public GenericAutoContainer(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String url = parameter.getCryptedUrl();
        if (!JsonConfig.create(LinkCrawlerConfig.class).isAutoImportContainer()) {
            ret.add(createDownloadlink(url));
        } else {
            final String type = new Regex(url, this.getSupportedLinks()).getMatch(0);
            URLConnectionAdapter con = null;
            File containerTemp = null;
            try {
                con = br.openGetConnection(url);
                if (con.isOK()) {
                    boolean seemsValidContainer = StringUtils.containsIgnoreCase(con.getContentType(), type);
                    seemsValidContainer = seemsValidContainer | (con.isContentDisposition() && StringUtils.containsIgnoreCase(Plugin.getFileNameFromHeader(con), type));
                    if (seemsValidContainer) {
                        containerTemp = org.appwork.utils.Application.getResource("tmp/autocontainer/" + System.nanoTime() + "." + type);
                        br.downloadConnection(containerTemp, con);
                        if (containerTemp.exists() && containerTemp.length() > 100) {
                            ret.addAll(loadContainerFile(containerTemp));
                        }
                    }
                }
            } catch (final IOException e) {
                logger.log(e);
                if (ret.size() == 0) {
                    ret.add(createDownloadlink(url));
                }
            } finally {
                if (containerTemp != null && containerTemp.exists()) {
                    containerTemp.delete();
                }
                if (con != null) {
                    con.disconnect();
                }
            }
            if (containerTemp != null && ret.size() == 0) {
                ret.add(createDownloadlink(url));
            }
        }
        return ret;
    }

}
