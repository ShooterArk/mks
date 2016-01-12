package org.mks.sslcertificate;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import org.mks.makesbridge.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.security.KeyChain;
import android.util.Log;

public class SSLCertificateErrorDialogActivity extends Activity {

    private static final String TAG = SSLCertificateErrorDialogActivity.class
            .getSimpleName();
    /** Key to send certificate via Intent between activities */
    private static final String CERTIFICATE_INTENT_EXTRA = "ssl_certificate";
    /** Key to send failing url via Intent between activities */
    private static final String FAILING_URL_INTENT_EXTRA = "failing_url";
    /** Request code for install certificate */
    private static final int INSTALL_CERTIFICATE = 100;
    private AlertDialog mCertificateDialog;
    /**
     * Certificate which needs to added to trust zone.
     */
    private X509Certificate mX509Certificate;
    /**
     * Url which is being failed for the SSL handshake
     */
    private String mFailingUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This is UI less Activity. Layout should not be set.
        // Read certificate intent and install
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            Log.d(TAG, "Can not show dialog, intent is null");
            finish();
            return;
        }
        this.mX509Certificate = (X509Certificate) intent
                .getSerializableExtra(CERTIFICATE_INTENT_EXTRA);
        this.mFailingUrl = (String) intent.getStringExtra(FAILING_URL_INTENT_EXTRA);
        if ((this.mX509Certificate == null) || (this.mFailingUrl == null)) {
            Log.d(TAG,
                    "Can not show dialog, certificate or failingurl is null");
            finish();
            return;
        }
        // Inform user for certificate error
        if ((mCertificateDialog == null)
                || (mCertificateDialog.isShowing() == false)) {
            // Show dialog only when if it it not showing.
            // Certificate will be updated, and will be read
            // from dialog when click on ok. So no need to
            // dismiss current dialog.
            showSSLCertificateAcceptDialog();
        }
    }

    @Override
    public void onBackPressed() {
        // Prevent back press
    }

    @Override
    protected void onDestroy() {
        if ((mCertificateDialog != null)
                && (mCertificateDialog.isShowing() == true)) {
            mCertificateDialog.dismiss();
        }
        super.onDestroy();
    }

    /**
     * Shows an alert dialog about SSL certificate issue. If user proceed,
     * certificate will be added to trust zone, and this dialog will not be
     * shown for same certificate.
     */
    private void showSSLCertificateAcceptDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(
                SSLCertificateErrorDialogActivity.this);
        builder.setTitle("Untrusted Certificate");
        builder.setMessage("New certificate detected");
        builder.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        installCertificate();
                    }
                });
        builder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        // TODO Retry the failing url
                        finish();
                    }
                });
        mCertificateDialog = builder.create();
        mCertificateDialog.setCancelable(false);
        mCertificateDialog.show();
    }

    /**
     * Install {@link X509Certificate} to trust zone. First this method will try
     * to add certificate from background and on fail it will show a dialog to
     * add certificate. This method must be called from an Activity, as it need
     * an activity instance.
     */
    
    private void installCertificate() {
        X509Certificate certificate = SSLCertificateErrorDialogActivity.this.mX509Certificate;
        if (certificate != null) {
            byte[] encodedCert = null;
            try {
                encodedCert = certificate.getEncoded();
            } catch (CertificateEncodingException e) {
                e.printStackTrace();
            }
            if (encodedCert != null) {
                installUsingIntent(encodedCert, INSTALL_CERTIFICATE);
            }
        } else {
            // TODO Retry the failing url
            finish();
        }
    }

    /**
     * Install certificate to trust zone using intent. User action will be
     * required while installing.
     * 
     * @param encodedCert
     *            of {@link X509Certificate}
     * @param requestCode
     */
    
    private void installUsingIntent(byte[] encodedCert, int requestCode) {
        Intent intent = KeyChain.createInstallIntent();
        // Default Alias name. User can change it.
        intent.putExtra(KeyChain.EXTRA_NAME, "MY Certificate");
        intent.putExtra(KeyChain.EXTRA_CERTIFICATE, encodedCert);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case INSTALL_CERTIFICATE:
            // No matter if action was success or not, retry to connect with
            // failed url and finish this activity.
            // You can retry the failiing url
            finish();
            break;

        default:
            break;
        }
    }

    /**
     * Show {@link SSLCertificateErrorDialogActivity} to inform user that, while
     * communicating to server there is untrusted certificate exception. User
     * can take action, certificate will be added to trust zone if user proceed.
     * 
     * @param context
     * @param certificate
     *            {@link X509Certificate} to be added to trust zone.
     * @param failingUrl
     *            is an url for SSL certificate error occurred, purpose of this
     *            url is to retry the same url after user action either
     *            cancelled or proceed.
     */
    public static void show(Context context, X509Certificate certificate,
            String failingUrl) {
        Context appContext = context.getApplicationContext();
        Intent intent = new Intent(appContext,
                SSLCertificateErrorDialogActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra(CERTIFICATE_INTENT_EXTRA, certificate);
        intent.putExtra(FAILING_URL_INTENT_EXTRA, failingUrl);
        appContext.startActivity(intent);
    }
}
