package com.rankersprep.rankerspreplearning.ui.notifications;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rankersprep.rankerspreplearning.PaymentDetailAdapter;
import com.rankersprep.rankerspreplearning.R;
import com.rankersprep.rankerspreplearning.databinding.ActivityPaymentDetailBinding;
import com.rankersprep.rankerspreplearning.databinding.ActivitySigninBinding;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class PaymentDetailActivity extends AppCompatActivity {

    ActivityPaymentDetailBinding binding;
    ArrayList<String> menteeNames,menteeUIDs,planName;
    ArrayList<Integer> amountBreakdown;
    String mentorName,UID,amount,upiId;
    ListView lv;
    private DatabaseReference mDatabase;

    public void payNow(){
        String key =mDatabase.child("payments").push().getKey();
        HashMap<String,Object> map = new HashMap<>();
        map.put("name",mentorName);
        map.put("amount",amount);
        Calendar calendar = Calendar.getInstance();
        String myFormat = "dd/MM/yyyy"; //In which you need put here
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat);
        String date =sdf.format(calendar.getTime());
        map.put("date",date);
        mDatabase.child("payments").child(key).updateChildren(map, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull @NotNull DatabaseReference ref) {
                if(error==null){
                    for(int i=0;i< planName.size();i++) {
                        String key1 = mDatabase.child("payments").child(key).child("details").push().getKey();
                        HashMap<String,Object> map1= new HashMap<>();
                        map1.put("mentee",menteeNames.get(i));
                        map1.put("amount",amountBreakdown.get(i));
                        map1.put("plan",planName.get(i));
                        mDatabase.child("payments").child(key).child("details").child(key1).updateChildren(map1);
                        mDatabase.child("users").child(UID).child("payments").child(key1).updateChildren(map1);
                        mDatabase.child("users").child(UID).child("payments").child(key1).child("date").setValue(date);
                    }
                }
            }
        });
        for(int i=0;i< planName.size();i++){
            int finalI = i;
            mDatabase.child("users").child(UID).child("mentees").child(menteeUIDs.get(i)).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull @NotNull Task<DataSnapshot> task) {
                    if(task.isSuccessful()){
                        DataSnapshot snapshot = task.getResult();
                        int monthsRemaining = Integer.parseInt(snapshot.child("monthsRemaining").getValue().toString());
                        int nextPayment = Integer.parseInt(snapshot.child("nextPaymentMonth").getValue().toString());
                        if(monthsRemaining>1){
                            mDatabase.child("users").child(UID).child("mentees").child(menteeUIDs.get(finalI)).child("monthsRemaining").setValue(String.valueOf(monthsRemaining-1));
                            mDatabase.child("users").child(UID).child("mentees").child(menteeUIDs.get(finalI)).child("nextPaymentMonth").setValue(String.valueOf(nextPayment+1));
                            mDatabase.child("mentees").child(menteeUIDs.get(finalI)).child("monthsRemaining").setValue(String.valueOf(monthsRemaining-1));
                        }else{
                            mDatabase.child("users").child(UID).child("mentees").child(menteeUIDs.get(finalI)).removeValue();
                            mDatabase.child("mentees").child(menteeUIDs.get(finalI)).child("monthsRemaining").setValue(0);
                        }
                        onBackPressed();
                    }
                }
            });
        }
    }

     public  void payUsingUpi(String amount, String upiId, String name, String note) {

        Uri uri = Uri.parse("upi://pay").buildUpon()
                .appendQueryParameter("pa", upiId)
                .appendQueryParameter("pn", name)
                .appendQueryParameter("tn", note)
                .appendQueryParameter("am", amount)
                .appendQueryParameter("cu", "INR")
                .appendQueryParameter("mc", "")
                .appendQueryParameter("tr", "25584584")
                .build();


        Intent upiPayIntent = new Intent(Intent.ACTION_VIEW);
        upiPayIntent.setData(uri);

        // will always show a dialog to user to choose an app
        Intent chooser = Intent.createChooser(upiPayIntent, "Pay with");

        // check if intent resolves
        if(null != chooser.resolveActivity(getPackageManager())) {
            startActivityForResult(chooser, 0);
        } else {
            Toast.makeText(getApplicationContext(),"No UPI app found, please install one to continue",Toast.LENGTH_SHORT).show();
        }

    }

    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = listView.getPaddingTop() + listView.getPaddingBottom();

        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            if (listItem instanceof ViewGroup) {
                listItem.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }

            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }


    public void payNow(View view){

        
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("Pay "+mentorName+"?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage("Do you want to pay now?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String note = "For these mentees:- ";
                        for (String s : menteeNames){
                            note+=s+", ";
                        }
                        payUsingUpi(amount,upiId,mentorName,note);
                    }
                })
                .setNegativeButton("No",null)
                .show();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPaymentDetailBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        Intent intent = getIntent();
        menteeNames = new ArrayList<>();
        menteeUIDs= new ArrayList<>();
        amountBreakdown = new ArrayList<>();
        planName = new ArrayList<>();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.keepSynced(true);

        mentorName = intent.getStringExtra("mentorName");
        UID = intent.getStringExtra("UID");
        amount = intent.getStringExtra("amount");


        binding.mentorNamePaymentTV.setText(mentorName);
        binding.roleTV.setText("Mentor");
        binding.totalTV.setText("₹ "+amount);

        menteeNames = intent.getStringArrayListExtra("menteeNames");
        menteeUIDs = intent.getStringArrayListExtra("menteeUIDs");
        amountBreakdown = intent.getIntegerArrayListExtra("amountBreakdown");

        binding.loading1.setVisibility(View.INVISIBLE);


        binding.loading1.setVisibility(View.VISIBLE);

        lv = binding.mentorPaymentLV;
        Activity activity = this;

        mDatabase.child("users").child(UID).child("upi").get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
            @Override
            public void onSuccess(DataSnapshot dataSnapshot) {
                upiId = dataSnapshot.getValue().toString();
            }
        });


        for(String uid : menteeUIDs){
            mDatabase.child("mentees").child(uid).child("plan").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull @NotNull Task<DataSnapshot> task) {
                    if(task.isSuccessful()){
                        Log.i("plan", task.getResult().toString());
                        planName.add(task.getResult().getValue().toString());
                        binding.loading1.setVisibility(View.INVISIBLE);
                        PaymentDetailAdapter adapter = new PaymentDetailAdapter(activity, menteeNames,amountBreakdown, planName);
                        lv.setAdapter(adapter);
                        setListViewHeightBasedOnChildren(lv);
                    }
                }
            });
        }

        binding.backToPayments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 0:
                if ((RESULT_OK == resultCode) || (resultCode == 11)) {
                    if (data != null) {
                        String trxt = data.getStringExtra("response");
                        Log.d("UPI", "onActivityResult: " + trxt);
                        ArrayList<String> dataList = new ArrayList<>();
                        dataList.add(trxt);
                        upiPaymentDataOperation(dataList);
                    } else {
                        Log.d("UPI", "onActivityResult: " + "Return data is null");
                        ArrayList<String> dataList = new ArrayList<>();
                        dataList.add("nothing");
                        upiPaymentDataOperation(dataList);
                    }
                } else {
                    Log.d("UPI", "onActivityResult: " + "Return data is null"); //when user simply back without payment
                    ArrayList<String> dataList = new ArrayList<>();
                    dataList.add("nothing");
                    upiPaymentDataOperation(dataList);
                }
                break;
        }
    }

    private void upiPaymentDataOperation(ArrayList<String> data) {
        if (isConnectionAvailable(this)) {
            String str = data.get(0);
            Log.d("UPIPAY", "upiPaymentDataOperation: "+str);
            String paymentCancel = "";
            if(str == null) str = "discard";
            String status = "";
            String approvalRefNo = "";
            String response[] = str.split("&");
            for (int i = 0; i < response.length; i++) {
                String equalStr[] = response[i].split("=");
                if(equalStr.length >= 2) {
                    if (equalStr[0].toLowerCase().equals("Status".toLowerCase())) {
                        status = equalStr[1].toLowerCase();
                    }
                    else if (equalStr[0].toLowerCase().equals("ApprovalRefNo".toLowerCase()) || equalStr[0].toLowerCase().equals("txnRef".toLowerCase())) {
                        approvalRefNo = equalStr[1];
                    }
                }
                else {
                    paymentCancel = "Payment cancelled by user.";
                }
            }

            if (status.equals("success")) {
                //Code to handle successful transaction here.
                Toast.makeText(this, "Transaction successful.", Toast.LENGTH_SHORT).show();
                payNow();
                Log.d("UPI", "responseStr: "+approvalRefNo);
            }
            else if("Payment cancelled by user.".equals(paymentCancel)) {
                Toast.makeText(this, "Payment cancelled by user.", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(this, "Transaction failed.Please try again", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Internet connection is not available. Please check and try again", Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean isConnectionAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
            if (netInfo != null && netInfo.isConnected()
                    && netInfo.isConnectedOrConnecting()
                    && netInfo.isAvailable()) {
                return true;
            }
        }
        return false;
    }

}