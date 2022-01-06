package org.openobd2.core.logger.bl

import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import org.openobd2.core.logger.ui.preferences.Preferences

class Wifi {

    private fun enable(context: Context) {
        val conf = WifiConfiguration().apply {
            val networkSSID = Preferences.getString(
                context,
                "pref.adapter.connection.tcp.wifi.ssid"
            );

            SSID = "\"" + networkSSID + "\""
            //            conf.wepKeys[0] = "\"" + networkPass + "\""
            //            conf.wepTxKeyIndex = 0
            //            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            //            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
            //            conf.preSharedKey = "\"" + networkPass + "\""

            allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            allowedAuthAlgorithms.clear();
            allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        }

        (context.getSystemService(Context.WIFI_SERVICE) as WifiManager).apply {
            setWifiEnabled(true)
            val networkId = addNetwork(conf)
            disableNetwork(connectionInfo.networkId)
            enableNetwork(networkId, true)
        }
    }
}