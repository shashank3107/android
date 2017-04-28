package com.genonbeta.TrebleShot.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.genonbeta.TrebleShot.helper.AwaitedFileReceiver;
import com.genonbeta.TrebleShot.helper.AwaitedFileSender;
import com.genonbeta.TrebleShot.helper.AwaitedTransaction;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by: veli
 * Date: 4/15/17 1:16 AM
 */

public class Transaction extends MainDatabase
{
	public static final String TAG = Transaction.class.getSimpleName();

	public static final String ACTION_TRANSACTION_REGISTERED = "com.genonbeta.TrebleShot.intent.action.TRANSACTION_REGISTERED";
	public static final String ACTION_TRANSACTION_UPDATED = "com.genonbeta.TrebleShot.intent.action.TRANSACTION_UPDATED";
	public static final String ACTION_TRANSACTION_REMOVED = "com.genonbeta.TrebleShot.intent.action.TRANSACTION_REMOVED";

	public enum Flag {
		PENDING,
		ERROR,
		RUNNING,
		RETRY
	};

	private ArrayBlockingQueue<AwaitedFileReceiver> mPendingReceivers = new ArrayBlockingQueue<AwaitedFileReceiver>(2000, true);

	public Transaction(Context context)
	{
		super(context);
	}

	public int acceptPendingReceivers(int acceptId)
	{
		int count = 0;

		Log.d(TAG, "Receiver count " + false + "; pending receiver count = " + getPendingReceivers().size() + "; copiedReceivers = " + getPendingReceivers().size());

		for (AwaitedFileReceiver receiver : getPendingReceivers())
		{
			Log.d(TAG, "Accept requested id = " + acceptId + "; current receivers id " + receiver.acceptId);

			if (receiver.acceptId != acceptId)
				continue;

			registerTransaction(receiver);
			getPendingReceivers().remove(receiver);

			count++;
		}

		Log.d(TAG, "After accepting pendingReceivers, current receivers count " + false);

		return count;
	}

	public boolean applyAccessPort(int requestId, int port)
	{
		ContentValues values = new ContentValues();
		values.put(FIELD_TRANSFER_ACCESSPORT, port);

		return updateTransaction(requestId, values) > 0;
	}

	public ArrayList<AwaitedFileReceiver> getPendingReceiversByAcceptId(int acceptId)
	{
		ArrayList<AwaitedFileReceiver> list = new ArrayList<AwaitedFileReceiver>();

		for (AwaitedFileReceiver receiver : getPendingReceivers())
			if (receiver.acceptId == acceptId)
				list.add(receiver);

		return list;
	}

	public ArrayBlockingQueue<AwaitedFileReceiver> getPendingReceivers()
	{
		return mPendingReceivers;
	}

	public ArrayList<AwaitedFileReceiver> getReceivers()
	{
		return getReceivers(new SQLQuery.Select(TABLE_TRANSFER)
				.setWhere(FIELD_TRANSFER_TYPE + "=?", String.valueOf(TYPE_TRANSFER_TYPE_INCOMING)));
	}

	public ArrayList<AwaitedFileReceiver> getReceivers(SQLQuery.Select select)
	{
		ArrayList<CursorItem> list = getTable(select);

		ArrayList<AwaitedFileReceiver> outputList = new ArrayList<>();

		for (CursorItem item : list)
			outputList.add(new AwaitedFileReceiver(item));

		return outputList;
	}

	public ArrayList<AwaitedFileSender> getSenders()
	{
		return getSenders(new SQLQuery.Select(TABLE_TRANSFER)
				.setWhere(FIELD_TRANSFER_TYPE + "=?", String.valueOf(TYPE_TRANSFER_TYPE_OUTGOING)));
	}

	public ArrayList<AwaitedFileSender> getSenders(SQLQuery.Select select)
	{
		ArrayList<CursorItem> list = getTable(select);

		ArrayList<AwaitedFileSender> outputList = new ArrayList<>();

		for (CursorItem item : list)
			outputList.add(new AwaitedFileSender(item));

		return outputList;
	}

	public CursorItem getTransaction(int requestId)
	{
		return getFirstFromTable(new SQLQuery.Select(TABLE_TRANSFER)
				.setWhere(FIELD_TRANSFER_ID + "=?", String.valueOf(requestId)));
	}

	public boolean registerTransaction(AwaitedTransaction transaction)
	{
		getWritableDatabase().insert(TABLE_TRANSFER, null, transaction.getDatabaseObject());
		getContext().sendBroadcast(new Intent(ACTION_TRANSACTION_REGISTERED));
		return getAffectedRowCount() > 0;
	}

	public int removePendingReceivers(int acceptId)
	{
		int count = 0;

		for (AwaitedFileReceiver receiver : getPendingReceivers())
		{
			if (receiver.acceptId != acceptId)
				continue;

			getPendingReceivers().remove(receiver);

			count++;
		}

		return count;
	}

	public boolean removeTransaction(AwaitedTransaction transaction)
	{
		return removeTransaction(transaction.requestId);
	}

	public boolean removeTransaction(int requestId)
	{
		getWritableDatabase().delete(TABLE_TRANSFER, FIELD_TRANSFER_ID + "=?", new String[]{String.valueOf(requestId)});
		getContext().sendBroadcast(new Intent(ACTION_TRANSACTION_REMOVED));
		return getAffectedRowCount() > 0;
	}

	public boolean removeTransactionGroup(AwaitedTransaction transaction)
	{
		return removeTransactionGroup(transaction.acceptId);
	}

	public boolean removeTransactionGroup(int acceptId)
	{
		getWritableDatabase().delete(TABLE_TRANSFER, FIELD_TRANSFER_ACCEPTID + "=?", new String[]{String.valueOf(acceptId)});
		getContext().sendBroadcast(new Intent(ACTION_TRANSACTION_REMOVED));
		return getAffectedRowCount() > 0;
	}

	public boolean transactionExists(int requestId)
	{
		return getFirstFromTable(new SQLQuery.Select(TABLE_TRANSFER).setWhere(FIELD_TRANSFER_ID + "=?", String.valueOf(requestId))) != null;
	}

	public boolean updateFlag(int requestId, Flag flag)
	{
		ContentValues values = new ContentValues();
		values.put(FIELD_TRANSFER_FLAG, flag.toString());

		return updateTransaction(requestId, values) > 0;
	}

	public long updateTransaction(AwaitedTransaction transaction)
	{
		return updateTransaction(transaction.requestId, transaction.getDatabaseObject());
	}

	public long updateTransaction(int requestId, ContentValues values)
	{
		getWritableDatabase().update(TABLE_TRANSFER, values, FIELD_TRANSFER_ID + "=?", new String[] {String.valueOf(requestId)});
		getContext().sendBroadcast(new Intent(ACTION_TRANSACTION_UPDATED));
		return getAffectedRowCount();
	}
}
