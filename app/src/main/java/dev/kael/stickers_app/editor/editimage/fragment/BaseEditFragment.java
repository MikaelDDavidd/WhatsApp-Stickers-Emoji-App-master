package dev.kael.stickers_app.editor.editimage.fragment;

import android.os.Bundle;
import androidx.fragment.app.Fragment;

import dev.kael.stickers_app.editor.editimage.EditImageActivity;

/**
 * Created by Vimal-CVS on 04/11/2020.
 */
public abstract class BaseEditFragment extends Fragment {
    protected EditImageActivity activity;

    protected EditImageActivity ensureEditActivity(){
        if(activity==null){
            activity = (EditImageActivity)getActivity();
        }
        return activity;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ensureEditActivity();
    }

    public abstract void onShow();

    public abstract void backToMain();
}//end class
