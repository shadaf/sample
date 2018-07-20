package com.mintwireless.mintegrate.console.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.mintwireless.mintegrate.console.CaptureSignature;
import com.mintwireless.mintegrate.console.R;
import com.mintwireless.mintegrate.console.utils.Logger;
import com.mintwireless.mintegrate.core.Session;
import com.mintwireless.mintegrate.core.exceptions.MintegrateError;
import com.mintwireless.mintegrate.core.exceptions.MintegrateException;
import com.mintwireless.mintegrate.core.models.ApplicationSelectionItem;
import com.mintwireless.mintegrate.core.requests.CardDetectionModeController;
import com.mintwireless.mintegrate.core.requests.RequestBase;
import com.mintwireless.mintegrate.core.requests.SubmitPaymentRequest;
import com.mintwireless.mintegrate.core.requests.SubmitRefundRequest;
import com.mintwireless.mintegrate.core.requests.VerifySignatureRequest;
import com.mintwireless.mintegrate.core.responses.BaseResponse;
import com.mintwireless.mintegrate.core.responses.GetTransactionDetailsResponse;
import com.mintwireless.mintegrate.core.responses.SubmitPaymentResponse;
import com.mintwireless.mintegrate.sdk.Mintegrate;

import java.util.ArrayList;

/**
 * Created by Jialian on 4/05/16.
 */
public class PaymentActivity extends AppCompatActivity implements SubmitPaymentRequest.onEventListenerChipnPin, ProcessDialogFragment.OnCancelCallback {

    private EditText editTextAmount, editTextAmountR;
    private EditText editTextNote, editTextNoteR;
    private Button btnSubmitPayment, btnSubmitPaymentR;
    private String authToken;
    private Session session;
    private ProcessDialogFragment processDialogFragment;
    private Toolbar toolbar;

    private boolean mIsFallBack = false;
    private boolean mIsFallForward = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        authToken = getIntent().getStringExtra("authtoken");
        editTextAmount = (EditText) findViewById(R.id.amount);
        editTextNote = (EditText) findViewById(R.id.note);
        btnSubmitPayment = (Button) findViewById(R.id.submit_payment);
        editTextAmountR = (EditText) findViewById(R.id.amountR);
        editTextNoteR = (EditText) findViewById(R.id.noteR);
        btnSubmitPaymentR = (Button) findViewById(R.id.submit_paymentR);
        btnSubmitPayment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitPayment();
            }
        });
        btnSubmitPaymentR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callrefund();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.payment_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_transactions)
            openTransactions();
        return true;
    }

    private void openTransactions() {
        Intent intentPayment = new Intent(this, TransactionsActivity.class);
        intentPayment.putExtra("authtoken", authToken);
        startActivity(intentPayment);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Mintegrate.handleGoingToForeground();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Mintegrate.handleGoingToBackground();
    }

    private void submitPayment() {

        try {

            SubmitPaymentRequest submitPaymentRequest = new SubmitPaymentRequest();

            // amount in cents
            submitPaymentRequest.setAmount(editTextAmount.getText().toString()); // required
            submitPaymentRequest.setNotes(editTextNote.getText().toString());
            submitPaymentRequest.setAuthToken(authToken); // required

            // you can change the card detection mode by calling setCardDetectionMode
            // default is CardDetectionModeAll if not set
            submitPaymentRequest.setCardDetectionMode(RequestBase.CARD_DETECTION_MODE.CARD_DETECTION_MODE_ALL);
            session = Mintegrate.submitPayment(submitPaymentRequest, this, this);
            processDialogFragment = ProcessDialogFragment.newInstance("initialising transaction");
            processDialogFragment.setCancelCallback(this);
            processDialogFragment.setCancelable(false);

            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.add(processDialogFragment, "loadingFragment").commit();
            session.next();

        } catch (MintegrateException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onPaymentProgress(Session session) {

        // SDK will call this callback during the payment process with relevant status message

        processDialogFragment.setProcessMessage("Transaction is processing...");
        processDialogFragment.setCancelProcess(false);
    }

    @Override
    public void onWaitForRemoveCard(Session session, SubmitPaymentResponse submitPaymentResponse) {

        // this will be called to instruct user to remove his/her card if it's a chip transaction
        processDialogFragment.setProcessMessage("Please remove card");
    }

    @Override
    public void onWaitForReaderConnection(Session session) {

        // callback is called when card reader is not connected or paired

        processDialogFragment.setProcessMessage("Waiting for reader...");
        processDialogFragment.setCancelProcess(true);
    }

    @Override
    public void onReaderConnected(Session session) {

        // when card reader was connected or paired
        // will only be called when reader was not connected initially, otherwise,
        // SDK will call either onUpdatingReader, onPrepareToWaitForCard, etc.

        processDialogFragment.setProcessMessage("Reader is connected...");
    }

    @Override
    public void onUpdatingReader(String s, float v) {

        // if the card reader needs to be updated then this callback will be called during
        // the update process and to report the progress

        processDialogFragment.setProcessMessage(s);
    }

    @Override
    public void onReaderStatusMessageReceived(String s) {
        processDialogFragment.setProcessMessage(s);
    }

    @Override
    public void onPrepareToWaitForCard(Session session, CardDetectionModeController cardDetectionModeController) {

        // when card reader is connected/paired and it is updated then it is ready to accept
        // card payments

        processDialogFragment.setProcessMessage("Please tap/insert/swipe card...");

        // Note: while on this state, the integrator can have an option to switch to
        // different card detection modes - CardDetectionModeAll, CardDetectionModeInsertOrSwipe, or CardDetectionModeContactless
        // For example, if you want to switch to 'insert or swipe' mode), just do following
        // cardDetectionModeController.setCardDetectionMode(RequestBase.CARD_DETECTION_MODE.CARD_DETECTION_MODE_CHIP);

        // then change the UI/message for user just to insert/swipe their card
    }

    @Override
    public void onCardApplicationSelection(Session session, ArrayList<ApplicationSelectionItem> arrayList) {
    }

    @Override
    public void onWaitForSignature(Session session, SubmitPaymentResponse submitPaymentResponse) {

        // called when the SDK needs signature for identification

        // implements your code to show signature pad and get signature
        // you should convert the signature(bitmap) to encode Base64 string then pass it to SDK

        // e.g.
        // String signatureBase64 = Base64.encodeToString( "your signature byte Array", Base64.DEFAULT);
        // session.nextWithParameter(signatureBase64);
    }

    @Override
    public void onWaitForSignatureVerification(Session session) {

        // called after onWaitForSignature and let the merchant to verify the customer's signature with a
        // verification PIN (default is 0000 and can be changed using the SDK's configure operation)

        VerifySignatureRequest request = new VerifySignatureRequest();

        // setCancelling set to YES and SDK will call onWaitForSignature again so integrator needs
        // to redisplay the customer signature screen
        request.setCancelling(false);

        // LastAttempt should be set to YES when this is the last try so that
        // SDK will be informed and call the onError callback when error occurs
        // and integrator will have to make sure not to allow user to try and enter a verification pin
        // and close the session accordingly. If the verification PIN is correct then SDK will proceed
        // with the next operation state.
        request.setLastAttempt(false);

        request.setPin("your pin");

        this.session.nextWithParamater(request);
    }

    @Override
    public void onWaitForSendReceipt(Session session, SubmitPaymentResponse submitPaymentResponse) {

        // implements your UI for users to enter email address or mobile number
        // then you can create SendReceiptRequest object with email address or mobile number

        // e.g.
        // SendReceiptRequest request = new SendReceiptRequest();
        // request.setAuthToken("auth token");
        // request.setEmail(email);
        // request.setSMS(sms);
        // session.nextWithParameter(request);
        // or you can just call session.next() if you want to continue without sending receipt

        this.session.next();
    }

    @Override
    public void onError(Session session, MintegrateError.Error error) {
        Logger.logDebug(PaymentActivity.class, "onError: " + error.getMessage());

        // handle errors
        // please refer to the SDK's documentation for more detail about the error code
        switch (error.getCode()) {
            case MintegrateError.ERROR_COMMON_CARD_READER_NOT_CONNECTED:
                processDialogFragment.setProcessMessage("Waiting for reader...");
                break;
            case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_PIN_MISMATCH:
                break;
            case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_CARD_READ_ERROR: {
                String message = "Please Insert or Swipe Card";
                if (mIsFallBack) {
                    message = "Please Swipe Card";
                } else if (mIsFallForward) {
                    message = "Please Insert Card";
                }
                processDialogFragment.setProcessMessage(message);
                break;
            }
            case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_FALLBACK: {
                mIsFallBack = true;
                processDialogFragment.setProcessMessage("Please Swipe Card");
                break;
            }
            case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_FALLFORWARD: {
                mIsFallForward = true;
                processDialogFragment.setProcessMessage("Please Insert Card");
                break;
            }

            case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_CONTACTLESS_TRANSACTION_DECLINED_FALLBACK_TO_CHIP: {
                processDialogFragment.dismiss();
                closeSession();
                Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                break;
            }
            case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_CONTACTLESS_FALLBACK_TO_CHIP: {
                processDialogFragment.setProcessMessage("Please Insert Card");
                Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                break;
            }
            case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_CONTACTLESS_NOT_SUPPORTED: {
                processDialogFragment.setProcessMessage("Please Insert or Swipe Card");
                Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                break;
            }
            case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_CONTACTLESS_FALLBACK_TO_CHIP_OR_SWIPE: {
                processDialogFragment.setProcessMessage("Please Insert or Swipe Card");
                break;
            }
            case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_WAIT_FOR_ACCOUNT_SELECTION_TIME_OUT:
            case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_USER_CANCELLED:
            case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_WAIT_FOR_PIN_TIME_OUT:
            case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_CARD_REMOVED:
            case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_TRANSACTION_CANCEL_FAILED:
            case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_TRANSACTION_UNKNOWN_STATUS: {
                Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                processDialogFragment.dismiss();
                closeSession();
                break;
            }
            case MintegrateError.ERROR_SUBMIT_REFUND_WAITING_FOR_MERCHANT_SIGNATURE_TIMEOUT: {
                Toast.makeText(this, "Transaction Cancelled.Timeout waiting for merchant signature.", Toast.LENGTH_LONG).show();
                processDialogFragment.dismiss();
                closeSession();
                break;
            }
            case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_WAITING_FOR_CARD_TIMEOUT: {
                Toast.makeText(this, "Time out waiting for card", Toast.LENGTH_LONG).show();
                processDialogFragment.dismiss();
                closeSession();
                break;
            }
            case MintegrateError.ERROR_SEND_RECEIPT:
            case MintegrateError.ERROR_SEND_RECEIPT_PRINTER_NOT_CONNECTED:
            case MintegrateError.ERROR_GET_USER_RECEIPT:
            default:
                Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                processDialogFragment.dismiss();
                closeSession();
        }
    }

    @Override
    public void onCompletion(Session session, BaseResponse baseResponse) {
        // Transaction is finished
        // always keep in mind, close the session
        SubmitPaymentResponse submitPaymentResponse = (SubmitPaymentResponse) baseResponse;
        SubmitPaymentResponse.Status status = submitPaymentResponse.getTransactionStatus();
        processDialogFragment.dismiss();
        closeSession();
        String message = null;
        switch (status) {
            case APPROVED:
                message = "Transaction Approved";
                editTextAmountR.setText(submitPaymentResponse.getTransactionAmount());
                editTextNoteR.setText(submitPaymentResponse.getTransactionRequestID());
                break;
            case DECLINED:
                message = "Transaction Declined";
                break;
            case CANCELLED:
                message = "Transaction Cancelled";
        }

        Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();

    }

    @Override
    public void onCancel() {
        closeSession();
    }

    private void closeSession() {
        Logger.logInfo(PaymentActivity.class, "Close Session");
        if (session != null)
            session.close();
    }

    //Refund scenario
    private void callrefund(){
        SubmitRefundRequest refundRequest = new SubmitRefundRequest();
        refundRequest.setAmount(editTextAmountR.getText().toString());
        refundRequest.setRefundableAmount(editTextAmountR.getText().toString());
        refundRequest.setNotes("first refund");
        refundRequest.setRefundSourceTransactionRequestId(editTextNoteR.getText().toString());
        refundRequest.setAuthToken(authToken);


        //Callback
        SubmitRefundRequest.onRefundEventListenerWithMerchantSignature callback = new SubmitRefundRequest.onRefundEventListenerWithMerchantSignature() {


            @Override
            public void onError(Session session, MintegrateError.Error error) {
                switch (error.getCode()) {
                    case MintegrateError.ERROR_COMMON_CARD_READER_NOT_CONNECTED:
                        processDialogFragment.setProcessMessage("Waiting for reader...");
                        break;
                    case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_PIN_MISMATCH:
                        break;
                    case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_CARD_READ_ERROR: {
                        String message = "Please Insert or Swipe Card";
                        if (mIsFallBack) {
                            message = "Please Swipe Card";
                        } else if (mIsFallForward) {
                            message = "Please Insert Card";
                        }
                        processDialogFragment.setProcessMessage(message);
                        break;
                    }
                    case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_FALLBACK: {
                        mIsFallBack = true;
                        processDialogFragment.setProcessMessage("Please Swipe Card");
                        break;
                    }
                    case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_FALLFORWARD: {
                        mIsFallForward = true;
                        processDialogFragment.setProcessMessage("Please Insert Card");
                        break;
                    }

                    case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_CONTACTLESS_TRANSACTION_DECLINED_FALLBACK_TO_CHIP: {
                        processDialogFragment.dismiss();
                        closeSession();
                        Toast.makeText(PaymentActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                        break;
                    }
                    case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_CONTACTLESS_FALLBACK_TO_CHIP: {
                        processDialogFragment.setProcessMessage("Please Insert Card");
                        Toast.makeText(PaymentActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                        break;
                    }
                    case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_CONTACTLESS_NOT_SUPPORTED: {
                        processDialogFragment.setProcessMessage("Please Insert or Swipe Card");
                        Toast.makeText(PaymentActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                        break;
                    }
                    case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_CONTACTLESS_FALLBACK_TO_CHIP_OR_SWIPE: {
                        processDialogFragment.setProcessMessage("Please Insert or Swipe Card");
                        break;
                    }
                    case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_WAIT_FOR_ACCOUNT_SELECTION_TIME_OUT:
                    case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_USER_CANCELLED:
                    case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_WAIT_FOR_PIN_TIME_OUT:
                    case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_CARD_REMOVED:
                    case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_TRANSACTION_CANCEL_FAILED:
                    case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_TRANSACTION_UNKNOWN_STATUS: {
                        Toast.makeText(PaymentActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                        processDialogFragment.dismiss();
                        closeSession();
                        break;
                    }
                    case MintegrateError.ERROR_SUBMIT_REFUND_WAITING_FOR_MERCHANT_SIGNATURE_TIMEOUT: {
                        Toast.makeText(PaymentActivity.this, "Transaction Cancelled.Timeout waiting for merchant signature.", Toast.LENGTH_LONG).show();
                        processDialogFragment.dismiss();
                        closeSession();
                        break;
                    }
                    case MintegrateError.ERROR_SUBMIT_PAYMENT_REFUND_WAITING_FOR_CARD_TIMEOUT: {
                        Toast.makeText(PaymentActivity.this, "Time out waiting for card", Toast.LENGTH_LONG).show();
                        processDialogFragment.dismiss();
                        closeSession();
                        break;
                    }
                    case MintegrateError.ERROR_SEND_RECEIPT:
                    case MintegrateError.ERROR_SEND_RECEIPT_PRINTER_NOT_CONNECTED:
                    case MintegrateError.ERROR_GET_USER_RECEIPT:
                    default:
                        Toast.makeText(PaymentActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                        processDialogFragment.dismiss();
                        closeSession();
                }
            }

            @Override
            public void onCompletion(Session session, BaseResponse baseResponse) {
                SubmitPaymentResponse submitPaymentResponse = (SubmitPaymentResponse) baseResponse;
                SubmitPaymentResponse.Status status = submitPaymentResponse.getTransactionStatus();
                processDialogFragment.dismiss();
                closeSession();
                String message = null;
                switch (status) {
                    case APPROVED:
                        message = "Transaction Approved";
                        break;
                    case DECLINED:
                        message = "Transaction Declined";
                        break;
                    case CANCELLED:
                        message = "Transaction Cancelled";
                }

                Toast toast = Toast.makeText(PaymentActivity.this, message, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }

            @Override
            public void onPrepareToWaitForCard(Session session, CardDetectionModeController cardDetectionModeController) {

            }

            @Override
            public void onCardApplicationSelection(Session session, ArrayList<ApplicationSelectionItem> arrayList) {
            }

            @Override
            public void onWaitForSignature(Session session, SubmitPaymentResponse submitPaymentResponse) {
                processDialogFragment.setProcessMessage("Waiting for sign...");
            }

            @Override
            public void onWaitForSignatureVerification(Session session) {
                processDialogFragment.setProcessMessage("Waiting for sign verification...");
                VerifySignatureRequest signatureVerificationReq = new VerifySignatureRequest();
                signatureVerificationReq.setPin("123456");
                signatureVerificationReq.setLastAttempt(false);
                signatureVerificationReq.setCancelling(false);
                session.nextWithParamater(signatureVerificationReq);
            }

            @Override
            public void onWaitForSendReceipt(Session session, SubmitPaymentResponse submitPaymentResponse) {
                processDialogFragment.setProcessMessage("Please send receipt");
                session.next();
            }

            @Override
            public void onWaitForRemoveCard(Session session, SubmitPaymentResponse submitPaymentResponse) {
                processDialogFragment.setProcessMessage("Please remove card");
            }

            @Override
            public void onWaitForReaderConnection(Session session) {
                processDialogFragment.setProcessMessage("Waiting for reader...");
            }

            @Override
            public void onReaderConnected(Session session) {
                processDialogFragment.setProcessMessage("reader connected...");
            }

            @Override
            public void onUpdatingReader(String s, float v) {
                processDialogFragment.setProcessMessage(s);
            }

            @Override
            public void onReaderStatusMessageReceived(String s) {
                processDialogFragment.setProcessMessage(""+s);
            }

            @Override
            public void onRefundProgress(Session session) {
                processDialogFragment.setProcessMessage("Refund is in progress...");
            }

            @Override
            public void onWaitForMerchantSignature(Session session, GetTransactionDetailsResponse getTransactionDetailsResponse) {
                processDialogFragment.setProcessMessage("Waiting for signature...");
                Intent intent1 = new Intent(PaymentActivity.this, CaptureSignature.class);
                intent1.putExtra("Ref", "");
                intent1.putExtra("Date", "");
                intent1.putExtra("Amount",  "");
                startActivityForResult(intent1, 1);
            }
        };
        try {
            session = Mintegrate.submitRefund(refundRequest, callback, this);
        } catch (MintegrateException e) {
            e.printStackTrace();
        }
        processDialogFragment = ProcessDialogFragment.newInstance("initialising transaction");
        processDialogFragment.setCancelCallback(this);
        processDialogFragment.setCancelable(false);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(processDialogFragment, "loadingFragment").commit();
        session.next();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 1){
            if(data != null){
                session.nextWithParamater(data.getStringExtra("base"));
            }
        }

    }
}
