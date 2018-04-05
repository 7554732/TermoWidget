package com.fomichev.termowidget;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.example.termowidget.R;

public class DelDataDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.del_data_title)
                .setMessage(R.string.del_data_mes)
                .setPositiveButton(R.string.yes, delDataClickListener)
                .setNegativeButton(R.string.no, delDataClickListener);
        // Create the AlertDialog object and return it
        return builder.create();
    }

    DialogInterface.OnClickListener delDataClickListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case Dialog.BUTTON_POSITIVE:
                    mListener.onDelDataDialogPositiveClick(DelDataDialogFragment.this, which);
                    break;
                case Dialog.BUTTON_NEGATIVE:
                    mListener.onDelDataDialogNegativeClick(DelDataDialogFragment.this, which);
                    break;
                default:
                    break;
            }
        }
    };

    public interface DelDataDialogListener {
        public void onDelDataDialogPositiveClick(DialogFragment dialog, int which);
        public void onDelDataDialogNegativeClick(DialogFragment dialog, int which);
    }

    // Use this instance of the interface to deliver action events
    DelDataDialogListener mListener;

    // Override the Fragment.onAttach() method to instantiate the DelDataDialogListener
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (DelDataDialogListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, log exception
            if (TermoWidget.isDebug) Log.e(TermoWidget.LOG_TAG ,"Doesn't implement DelDataDialogListener");
        }
    }
}