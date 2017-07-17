package fragments;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

import com.olranz.payfuel.spmyadmin.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import config.ClientData;
import config.ClientServices;
import config.ServerClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import simplebean.indexbean.SetIndexBean;
import simplebean.indexbean.SetIndexResponse;
import simplebean.pumpbean.NozzleBean;
import simplebean.pumpbean.PumpBean;
import simplebean.pumpbean.PumpList;
import simplebean.userbean.UserDetails;
import simplebean.userbean.UserResponse;
import utilities.UserListAdapter;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnSetIndexInteraction} interface
 * to handle interaction events.
 * Use the {@link SetIndex#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SetIndex extends Fragment implements UserListAdapter.UserListInteraction {
    private String tag = getClass().getSimpleName();
    private static final String ARG_USER_ID = "userId";
    private static final String ARG_BRANCH_ID = "branchId";

    private int userId;
    private int branchId;
    private int pumpAgentId;
    private Dialog dialog;

    List<UserDetails> userDetailsList;

    private PumpList pumpList;
    private List<PumpBean> pumpBeanList;
    private List<NozzleBean> nozzleBeanList;
    private List<Integer> pumpIds;
    private List<String> pumpNames;
    private List<Integer> nozzleIds;
    private List<String> nozzleNames;

    Map<String, List<NozzleBean>> pumpMap;

    private ArrayAdapter<String> pumpAdapter;
    private ArrayAdapter<String> nozzleAdapter;

    int selectedNozzleId,selectedPumpId;
    String selectedNozzleName, selectedPumpName;

    private Spinner mPumps, mNozzles;
    private TextView nozzleDetails;
    private TextView nozzleLabel;
    private TextView fragLbl, lbl;
    private EditText index;
    private Button submit;
    private Typeface font;

    private ProgressDialog progressDialog;

    private OnSetIndexInteraction onSetIndexInteraction;

    public SetIndex() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param userId Parameter 1.
     * @param branchId Parameter 2.
     * @return A new instance of fragment Dipping.
     */
    public static SetIndex newInstance(int userId, int branchId) {
        SetIndex fragment = new SetIndex();
        Bundle args = new Bundle();
        args.putInt(ARG_USER_ID, userId);
        args.putInt(ARG_BRANCH_ID, branchId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getInt(ARG_USER_ID);
            branchId = getArguments().getInt(ARG_BRANCH_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        onSetIndexInteraction.onSetIndexChangeTitle("Set Indexes Report");
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.set_index_layout, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        font=Typeface.createFromAsset(getContext().getAssets(), "font/ubuntu.ttf");
        progressDialog = new ProgressDialog(getContext(), R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);

        mPumps=(Spinner) view.findViewById(R.id.mPumps);
        mNozzles=(Spinner) view.findViewById(R.id.mNozzles);
        lbl = (TextView) view.findViewById(R.id.lbl);
        lbl.setTypeface(font);
        fragLbl=(TextView) view.findViewById(R.id.fragLbl);
        fragLbl.setTypeface(font, Typeface.BOLD);

        nozzleDetails=(TextView) view.findViewById(R.id.nozzleDetails);
        nozzleDetails.setTypeface(font, Typeface.BOLD);
        nozzleLabel=(TextView) view.findViewById(R.id.nLbl);
        nozzleLabel.setTypeface(font);
        index=(EditText) view.findViewById(R.id.index);
        index.setTypeface(font);

        submit=(Button) view.findViewById(R.id.submit);
        submit.setTypeface(font, Typeface.BOLD);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(TextUtils.isEmpty(index.getText().toString().trim()))
                    uiFeed("Set index before you submit");
                else{
                    if(selectedNozzleId != 0 && userId != 0){
                        try {
                            confirmDialog(new SetIndexBean(userId, branchId, selectedPumpId, selectedPumpName, selectedNozzleId, selectedNozzleName, Double.valueOf(index.getText().toString().trim())));
                        }catch (Exception e){
                            uiFeed(e.getMessage());
                            index.setError("revise index");
                        }
                        //confirmDialog(new SetIndexBean(userId, branchId, selectedPumpId, "", selectedNozzleId, "", Double.valueOf(index.getText().toString().trim())));
                    }else
                        uiFeed("Setting Index Could not be accomplished with invalid data");
                }
            }
        });

        loadPumpAgents();
    }

private void loadPumpAgents(){
    final TextView dialogTitle;
    final RecyclerView userList;
    ImageView close;
    final RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());;

    dialog = new Dialog(getContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    dialog.setCancelable(false);
    dialog.setCanceledOnTouchOutside(false);
    dialog.setContentView(R.layout.user_list);

    dialogTitle = (TextView) dialog.findViewById(R.id.dialogTitle);
    dialogTitle.setTypeface(font, Typeface.BOLD);
    userList = (RecyclerView) dialog.findViewById(R.id.recyclerView);
    close = (ImageView) dialog.findViewById(R.id.icClose);


    progressDialog.show();
    progressDialog.setMessage("Loading pump attendant");
    //get pumps from url
    try {
        ClientServices clientServices = ServerClient.getClient().create(ClientServices.class);
        Call<UserResponse> callService = clientServices.getBranchUsers(branchId);
        callService.enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                progressDialog.dismiss();
                //HTTP status code
                int statusCode = response.code();
                if(statusCode != 200){
                    uiGoToHome(response.message());
                    return;
                }
                Log.d(tag,"Server Result: \n"+new ClientData().mapping(response.body()));
                try{
                    Log.d(getClass().getSimpleName(),new ClientData().mapping(response.body()));

                    if(response.body().getStatusCode() == 100){
                        //dispatch Data
                        userDetailsList=response.body().getUserDetailsList();
                        userList.setHasFixedSize(true);
                        userList.setLayoutManager(mLayoutManager);
                        UserListAdapter adapter=new UserListAdapter(getContext(), SetIndex.this,userDetailsList, dialog);
                        userList.setAdapter(adapter);
                    }else{
                        //Notify User that an error happened
                        uiRefresh(response.body().getMessage());
                    }

                } catch (final Exception e) {
                    e.printStackTrace();
                    uiRefresh(e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                // Log error here since request failed
                Log.e(getClass().getSimpleName(), t.toString());
                uiRefresh("Connectivity failure");
            }
        });
    } catch (Exception e) {
        e.printStackTrace();
        uiGoToHome(e.getMessage());
    }

    close.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            dialog.dismiss();
            onSetIndexInteraction.onSetIndex(Dipping.newInstance(userId, branchId), 0 ,"Go to home");
        }
    });

//    close.setOnClickListener(new View.OnClickListener() {
//        @Override
//        public void onClick(View v) {
//            dialog.dismiss();
//            onSetIndexInteraction.onSetIndex(Dipping.newInstance(userId, branchId), 0 ,"Go to home");
//        }
//    });
    dialog.show();
}

    private void loadPumps(){
        progressDialog.show();
        progressDialog.setMessage("Loading Pumps");
        //get pumps from url
        try {
            ClientServices clientServices = ServerClient.getClient().create(ClientServices.class);
            Call<PumpList> callService = clientServices.getPumpList(userId);
            callService.enqueue(new Callback<PumpList>() {
                @Override
                public void onResponse(Call<PumpList> call, Response<PumpList> response) {
                    progressDialog.dismiss();
                    //HTTP status code
                    int statusCode = response.code();
                    if(statusCode != 200){
                        uiFeed(response.message());
                        return;
                    }
                    Log.d(tag,"Server Result: \n"+new ClientData().mapping(response.body()));
                    try{
                        Log.d(getClass().getSimpleName(),new ClientData().mapping(response.body()));
                        pumpList = response.body();

                        if(pumpList.getStatusCode() == 100){
                            //dispatch Data
                            dispatchData(pumpList);
                        }else{
                            //Notify User that an error happened
                            uiFeed(pumpList.getMessage());
                        }

                    } catch (final Exception e) {
                        uiFeed(e.getMessage());
                    }
                }

                @Override
                public void onFailure(Call<PumpList> call, Throwable t) {
                    // Log error here since request failed
                    Log.e(getClass().getSimpleName(), t.toString());
                    uiFeed(t.getMessage()+"");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            uiFeed(e.getMessage());
        }
    }

    private void dispatchData(PumpList pumpList){
        if(pumpList.getPumps() == null){
            uiFeed("Empty Pumps");
            return;
        }
        if(pumpList.getPumps().isEmpty()){
            uiFeed("Empty Pumps");
            return;
        }

        pumpBeanList=pumpList.getPumps();
        pumpMap = new HashMap<>();
        pumpIds=new ArrayList<>();
        pumpNames=new ArrayList<>();

        nozzleIds=new ArrayList<>();
        nozzleNames=new ArrayList<>();
        List<NozzleBean> nozzleBeanListTemp=new ArrayList<>();

        for(PumpBean pumpBean: pumpBeanList){
            pumpIds.add(pumpBean.getPumpId());
//            pumpNames.add(pumpBean.getPumpId()+". "+pumpBean.getPumpName());
            pumpNames.add(pumpBean.getPumpName());

            nozzleBeanListTemp=pumpBean.getNozzles();
            List<NozzleBean> nozzleTemp=new ArrayList<>();
            for(NozzleBean nozzleBean: nozzleBeanListTemp){
                    nozzleTemp.add(nozzleBean);
            }
//            pumpMap.put(pumpBean.getPumpId()+". "+pumpBean.getPumpName(),nozzleTemp);
            pumpMap.put(pumpBean.getPumpName(),nozzleTemp);
        }

        populatePumpSpinner(pumpNames);
    }

    private void populatePumpSpinner(List<String> pumps){
        if(pumps.isEmpty()){
            uiFeed("Empty Pumps");
            return;
        }

        pumpAdapter=new ArrayAdapter<>(getContext(),R.layout.pump_style, pumps);
        mPumps.setAdapter(pumpAdapter);
        mPumps.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                selectedPumpId=pumpIds.get(position);
                selectedPumpName=pumpNames.get(position);
                try{
                    populateNozzleSpinner(pumpMap.get(pumpNames.get(position)));
                }catch (Exception e){
                    uiFeed(e.getMessage());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void populateNozzleSpinner(List<NozzleBean> nozzles){
        if(nozzles.isEmpty()){
            uiFeed("Empty Nozzles");
            return;
        }

        nozzleNames.clear();
        nozzleIds.clear();
        for(NozzleBean nozzleBean: nozzles){
//            nozzleNames.add(nozzleBean.getNozzleId()+". "+nozzleBean.getNozzleName());
            nozzleNames.add(nozzleBean.getNozzleName());
            nozzleIds.add(nozzleBean.getNozzleId());
        }

        if(nozzleNames.isEmpty()){
            uiFeed("Empty Nozzles");
            return;
        }

        nozzleBeanList=new ArrayList<>();
        nozzleBeanList=nozzles;
        nozzleAdapter=new ArrayAdapter<>(getContext(),R.layout.nozzle_style, nozzleNames);
        mNozzles.setAdapter(nozzleAdapter);
        mNozzles.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                selectedNozzleId=nozzleIds.get(position);
                selectedNozzleName=nozzleNames.get(position);
                try{

                    NozzleBean nozzleBean=nozzleBeanList.get(position);
                    if(nozzleBean.getStatus() == 8){
                        uiFeed("Nozzle "+nozzleBean.getNozzleName()+" taken by\n"+nozzleBean.getUserName());
                    }else if(nozzleBean.getStatus() != 8 && nozzleBean.getStatus() != 7){
                        uiFeed("Nozzle out of service\n"+nozzleBean.getUserName());
                    }
                    if(nozzleBean.getStatus() == 7){
                        submit.setEnabled(true);
                    }else if(nozzleBean.getStatus() == 8){
                        submit.setEnabled(false);
                    }
                        populateNozzleDetails(nozzleBean);

                }catch (Exception e){
                    uiFeed(e.getMessage());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void populateNozzleDetails(NozzleBean nozzleBean){
        String lbl = "SELLING\n"+"INDEXES";
        String value = nozzleBean.getProductName()+"\n"+nozzleBean.getNozzleIndex()+"";
//        String details="Nozzle: "+nozzleBean.getNozzleName()+"\n";
//        details += "Current Index: "+nozzleBean.getNozzleIndex()+"\n";
//        details += "Selling: "+nozzleBean.getProductName()+"\n";
        nozzleDetails.setText(value);
        nozzleLabel.setText(lbl);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnSetIndexInteraction) {
            onSetIndexInteraction = (OnSetIndexInteraction) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    private void confirmDialog(final SetIndexBean setIndexBean){
//        String content="New Value entry for: \n\n";
//        content +="Pump Name: "+setIndexBean.getPumpName()+"\n";
//        content += "Nozzle Name: "+setIndexBean.getNozzleName()+"\n";
//        content += "New INDEXES \n"+setIndexBean.getIndex();

        try {
            final Dialog dialog = new Dialog(getContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            dialog.setContentView(R.layout.confirm_box);

            final TextView boxTitle = (TextView) dialog.findViewById(R.id.dialogTitle);
            boxTitle.setTypeface(font, Typeface.BOLD);
            ImageView close = (ImageView) dialog.findViewById(R.id.icClose);
            Button cancel = (Button) dialog.findViewById(R.id.cancel);
            cancel.setTypeface(font, Typeface.BOLD);
            Button ok = (Button) dialog.findViewById(R.id.ok);
            ok.setTypeface(font, Typeface.BOLD);
            LinearLayout boxContent = (LinearLayout) dialog.findViewById(R.id.dialogContent);

            close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                }
            });

            boxTitle.setText("Confirm Indexes");

            ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    proceed(setIndexBean);
                    dialog.dismiss();
                }
            });

            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                }
            });

            //setting box content
//            int id = View.generateViewId();
            TextView lbl = new TextView(getContext());
            lbl.setTypeface(font);
            lbl.setText("Pump Name");
            lbl.setTextSize(21);
            lbl.setId(View.generateViewId());
            lbl.setLayoutParams(new LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
            boxContent.addView(lbl);

            TextView lblValue = new TextView(getContext());
            lblValue.setTypeface(font, Typeface.BOLD);
            lblValue.setText(setIndexBean.getPumpName()+"\n");
            lblValue.setTextSize(27);
            lblValue.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            lblValue.setId(View.generateViewId());
            lblValue.setLayoutParams(new LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
            boxContent.addView(lblValue);

            lbl = new TextView(getContext());
            lbl.setTypeface(font);
            lbl.setText("Nozzle Name");
            lbl.setTextSize(21);
            lbl.setId(View.generateViewId());
            lbl.setLayoutParams(new LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
            boxContent.addView(lbl);

            lblValue = new TextView(getContext());
            lblValue.setTypeface(font, Typeface.BOLD);
            lblValue.setText(setIndexBean.getNozzleName()+"\n");
            lblValue.setTextSize(27);
            lblValue.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            lblValue.setId(View.generateViewId());
            lblValue.setLayoutParams(new LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
            boxContent.addView(lblValue);

            lbl = new TextView(getContext());
            lbl.setTypeface(font);
            lbl.setText("Indexes");
            lbl.setTextSize(21);
            lbl.setId(View.generateViewId());
            lbl.setLayoutParams(new LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
            boxContent.addView(lbl);

            lblValue = new TextView(getContext());
            lblValue.setTypeface(font, Typeface.BOLD);
            lblValue.setText(setIndexBean.getIndex()+"\n");
            lblValue.setTextSize(27);
            lblValue.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            lblValue.setId(View.generateViewId());
            lblValue.setLayoutParams(new LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
            boxContent.addView(lblValue);

            dialog.show();


//            final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
//            builder.setMessage(content)
//                    .setTitle("Notification...");
//            // Add the buttons
//            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                public void onClick(DialogInterface dialog, int id) {
//                    proceed(setIndexBean);
//                    dialog.dismiss();
//                }
//            });
//            builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int i) {
//                    dialog.dismiss();
//                }
//            });
//            AlertDialog dialog = builder.create();
//            dialog.show();

        } catch (Exception e) {
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void proceed(final SetIndexBean setIndexBean){
        progressDialog.show();
        progressDialog.setMessage("Submitting Indexes");
        try {
            Log.d(tag,"Server Request: \n"+new ClientData().mapping(setIndexBean));
            ClientServices clientServices = ServerClient.getClient().create(ClientServices.class);
            Call<SetIndexResponse> callService = clientServices.setIndex(setIndexBean);
            callService.enqueue(new Callback<SetIndexResponse>() {
                @Override
                public void onResponse(Call<SetIndexResponse> call, Response<SetIndexResponse> response) {

                    //HTTP status code
                    index.setText("");
                    int statusCode = response.code();
                    if(statusCode != 200){
                        uiFeed(response.message());
                        return;
                    }
                    Log.d(tag,"Server Response: \n"+new ClientData().mapping(response.body()));
                    try{
                        SetIndexResponse setIndexResponse=response.body();
                        uiFeed(setIndexResponse.getMessage());
                        onSetIndexInteraction.onSetIndex(null, 4, "next");
                        loadPumps();
                    } catch (final Exception e) {
                        uiFeed(e.getMessage());
                    }
                }

                @Override
                public void onFailure(Call<SetIndexResponse> call, Throwable t) {
                    // Log error here since request failed
                    Log.e(getClass().getSimpleName(), t.toString());
                    uiFeed(t.getMessage()+"");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            uiFeed(e.getMessage());
        }
    }

    private void uiGoToHome(String feedBack){
        if(progressDialog != null)
            if(progressDialog.isShowing())
                progressDialog.dismiss();
        try {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.SimpleDialog);
            builder.setMessage(feedBack)
                    .setTitle(R.string.dialog_title);
            // Add the buttons
            builder.setPositiveButton("Back", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                    onSetIndexInteraction.onSetIndex(Dipping.newInstance(userId, branchId), 0 ,"Go to home");
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();

            TextView textView = (TextView) dialog.getWindow().findViewById(android.R.id.message);
            textView.setTypeface(font);
            TextView alertTitle = (TextView) dialog.getWindow().findViewById(R.id.alertTitle);
            alertTitle.setTypeface(font);
            Button button1 = (Button) dialog.getWindow().findViewById(android.R.id.button1);
            button1.setTypeface(font, Typeface.BOLD);

            Toast.makeText(getContext(), feedBack, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            onSetIndexInteraction.onSetIndex(Dipping.newInstance(userId, branchId), 0 ,"Go to home");
        }
    }

    private void uiRefresh(String feedBack){
        if(progressDialog != null)
            if(progressDialog.isShowing())
                progressDialog.dismiss();

        if(dialog != null)
            if(dialog.isShowing())
                dialog.dismiss();
        try {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.SimpleDialog);
            builder.setMessage(feedBack)
                    .setTitle(R.string.dialog_title);
            // Add the buttons
            builder.setPositiveButton("Refresh", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    loadPumpAgents();
                    dialog.dismiss();
                }
            });

            builder.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    onSetIndexInteraction.onSetIndex(Dipping.newInstance(userId, branchId), 0 ,"Go to home");
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();

            TextView textView = (TextView) dialog.getWindow().findViewById(android.R.id.message);
            textView.setTypeface(font);
            TextView alertTitle = (TextView) dialog.getWindow().findViewById(R.id.alertTitle);
            alertTitle.setTypeface(font);
            Button button1 = (Button) dialog.getWindow().findViewById(android.R.id.button1);
            button1.setTypeface(font, Typeface.BOLD);
            Button button2 = (Button) dialog.getWindow().findViewById(android.R.id.button2);
            button2.setTypeface(font, Typeface.BOLD);

            Toast.makeText(getContext(), feedBack, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            onSetIndexInteraction.onSetIndex(Dipping.newInstance(userId, branchId), 0 ,"Go to home");
        }
    }

    private void uiFeed(String feedBack){
        if(progressDialog != null)
            if(progressDialog.isShowing())
                progressDialog.dismiss();
        try {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.SimpleDialog);
            builder.setMessage(feedBack)
                    .setTitle(R.string.dialog_title);
            // Add the buttons
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();

            TextView textView = (TextView) dialog.getWindow().findViewById(android.R.id.message);
            textView.setTypeface(font);
            TextView alertTitle = (TextView) dialog.getWindow().findViewById(R.id.alertTitle);
            alertTitle.setTypeface(font);
            Button button1 = (Button) dialog.getWindow().findViewById(android.R.id.button1);
            button1.setTypeface(font, Typeface.BOLD);

            Toast.makeText(getContext(), feedBack, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            onSetIndexInteraction.onSetIndex(Dipping.newInstance(userId, branchId), 0 ,"Go to home");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        onSetIndexInteraction = null;
    }

    @Override
    public void onUserListInteraction(int status, int pumpAgentId, Object object, Dialog dialog) {
        if(status==1){
            this.pumpAgentId=pumpAgentId;
            if(dialog != null)
                if(dialog.isShowing())
                    dialog.dismiss();
            loadPumps();
            UserDetails userDetails=(UserDetails) object;
            fragLbl.setText(""+ userDetails.getFname());
        }else{
            if(dialog != null)
                if(dialog.isShowing())
                    dialog.dismiss();
            onSetIndexInteraction.onSetIndex(Dipping.newInstance(userId, branchId), 0 ,"Go to home");
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnSetIndexInteraction {
        void onSetIndex(Object object, int statusCode, String message);
        void onSetIndexChangeTitle(String title);
    }
}
