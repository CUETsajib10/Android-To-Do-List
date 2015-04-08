package benawad.com.todolist;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.style.StrikethroughSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

import benawad.com.todolist.adapters.ItemsArrayAdapter;
import benawad.com.todolist.contentprovider.NoteContentProvider;
import benawad.com.todolist.database.NoteTable;


public class NoteActivity extends ActionBarActivity {

    private final static String TAG = NoteActivity.class.getSimpleName();
    public final static int SLASHED = 1;
    public final static int UNSLASHED = 0;
    private static final StrikethroughSpan STRIKE_THROUGH_SPAN = new StrikethroughSpan();
    ItemsArrayAdapter mItemsArrayAdapter;
    ItemsArrayAdapter mFinishedItemsArrayAdapter;
    EditText mNewItemText;
    DynamicListView mItemsListView;
    ListView mFinishedItemsListView;
    ArrayList<String> mItems;
    ArrayList<String> mFinishedItems;
    //    FloatingActionButton fab;
    private Uri noteUri;
    public ArrayList<String> slashes;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_note);
        mItems = new ArrayList<String>();
        slashes = new ArrayList<>();
        mItemsArrayAdapter = new ItemsArrayAdapter(this, mItems, slashes);
        mFinishedItems = new ArrayList<>();
        mFinishedItemsArrayAdapter = new ItemsArrayAdapter(this, mFinishedItems, slashes);
        mItemsListView = (DynamicListView) findViewById(R.id.itemsListView);
        mFinishedItemsListView = (ListView) findViewById(R.id.finishedItems);
        mItemsListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

//        fab = (FloatingActionButton) findViewById(R.id.addNewItem);
//        fab.attachToListView(mItemsListView);
        mItemsListView.setAdapter(mItemsArrayAdapter);
        mItemsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView item = (TextView) view.findViewById(R.id.itemText);
                if (item.getPaintFlags() == 17) {
                    item.setPaintFlags(item.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                } else {
                    item.setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG);
                }
//                String text = item.getText().toString();
//                if(text.contains("<strike>")){
//                    int loc1 = text.indexOf("<strike>");
//                    int loc2 = text.indexOf("</strike>");
//                    item.setText(text.substring(loc1, loc2));
//                }
//                else{
//                    item.setText(Html.fromHtml("<strike>" + text + "</strike>"));
//                }
                //Html.fromHtml("<h2>Title</h2><br><p>Description here</p>");
            }
        });

        Bundle extras = getIntent().getExtras();

        // check from the saved Instance
        noteUri = (bundle == null) ? null : (Uri) bundle
                .getParcelable(NoteContentProvider.CONTENT_ITEM_TYPE);

        // Or passed from the other activity
        if (extras != null) {
            noteUri = extras
                    .getParcelable(NoteContentProvider.CONTENT_ITEM_TYPE);

            fillData(noteUri);

        }

        mItemsListView.setCheeseList(mItems);

    }

    private void fillData(Uri uri) {

        String[] projection = {NoteTable.COLUMN_ITEMS, NoteTable.COLUMN_SLASHED};
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, projection, null, null,
                    null);
        } catch (NullPointerException e) {
            Log.e(TAG, "NullPointerException caught: ", e);
        }
        if (cursor != null) {
            cursor.moveToFirst();

            String sItems = cursor.getString(cursor
                    .getColumnIndexOrThrow(NoteTable.COLUMN_ITEMS));

            String sSlashes = cursor.getString(cursor.getColumnIndexOrThrow(NoteTable.COLUMN_SLASHED));

            try {
                JSONArray jsonArray = new JSONArray(sItems);
                for (int i = 0; i < jsonArray.length(); i++) {
                    mItems.add((String) jsonArray.get(i));
                }
                JSONArray slashesJsonArray = new JSONArray(sSlashes);
                for (int i = 0; i < slashesJsonArray.length(); i++) {
                    slashes.add("" + slashesJsonArray.get(i));
                }
                mItemsArrayAdapter.notifyDataSetChanged();
            } catch (JSONException ignored) {
            }

            // always close the cursor
            cursor.close();
        }

    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveState();
        outState.putParcelable(NoteContentProvider.CONTENT_ITEM_TYPE, noteUri);
    }

    protected void onPause() {
        super.onPause();
        saveState();
    }

    private void saveState() {
        String note = new JSONArray(mItems).toString();
        ArrayList<Integer> slashes = new ArrayList<>();

        for (int i = 0; i < mItemsListView.getChildCount(); i++) {
            View row = mItemsListView.getChildAt(i);
            TextView textView = (TextView) row.findViewById(R.id.itemText);

            if (17 == textView.getPaintFlags()) {
                slashes.add(NoteActivity.SLASHED);
            } else {
                slashes.add(NoteActivity.UNSLASHED);
            }
        }

        String sSlashes = new JSONArray(slashes).toString();

        // only save if either summary or description
        // is available

        if (mItems.isEmpty()) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put(NoteTable.COLUMN_ITEMS, note);
        values.put(NoteTable.COLUMN_SLASHED, sSlashes);

        if (noteUri == null) {
            noteUri = getContentResolver().insert(NoteContentProvider.CONTENT_URI, values);
        } else {
            getContentResolver().update(noteUri, values, null, null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds mItems to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_note, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.addItem) {
            item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    addItem();
                    return false;
                }
            });
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void addItem() {

        if (mItems.size() < 100) {
            getDialog().show();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Max Items")
                    .setMessage("You have reached the maximum " +
                            "number of items (100) one note can hold.")
                    .setPositiveButton("OK", null);
            builder.show();
        }

    }

    public AlertDialog.Builder getDialog() {
        LayoutInflater li = LayoutInflater.from(this);
        LinearLayout newNoteBaseLayout = (LinearLayout) li.inflate(R.layout.new_item_dialog, null);

        mNewItemText = (EditText) newNoteBaseLayout.getChildAt(0);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mItems.add(mNewItemText.getText().toString());
                slashes.add("" + NoteActivity.UNSLASHED);
                mItemsArrayAdapter.notifyDataSetChanged();
            }
        });
        builder.setNegativeButton("Cancel", null)
                .setTitle("New Item");

        builder.setView(newNoteBaseLayout);
        return builder;
    }

    public void deleteItem(int position) {
        mItems.remove(position);
        slashes.remove(position);
        mItemsArrayAdapter.notifyDataSetChanged();
    }

    public void editItem(final int position) {
        AlertDialog.Builder builder = getDialog();
        mNewItemText.setText(mItems.get(position));
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mItems.set(position, mNewItemText.getText().toString());
                mItemsArrayAdapter.notifyDataSetChanged();
            }
        });
        builder.show();
    }

    public void uncheckAll(View view) {
        for (int i = 0; i < mItemsListView.getChildCount(); i++) {
            View row = mItemsListView.getChildAt(i);
            TextView textView = (TextView) row.findViewById(R.id.itemText);

            textView.setPaintFlags(textView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));

        }

    }
}