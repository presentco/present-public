package co.present.present.feature

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import android.util.Log
import android.view.MenuItem
import android.view.View
import co.present.present.BaseActivity
import co.present.present.R
import co.present.present.extensions.applyFlowableSchedulers
import co.present.present.feature.discovery.BaseCityItem
import co.present.present.feature.discovery.CityItem
import co.present.present.feature.discovery.NearbyCityItem
import co.present.present.location.LocationDataProvider
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.OnItemClickListener
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_city_picker.*
import kotlinx.android.synthetic.main.toolbar.*
import javax.inject.Inject


class CityPickerActivity : BaseActivity(), OnItemClickListener {

    private val TAG = javaClass.simpleName
    @Inject lateinit var citiesViewModel: CitiesViewModel
    @Inject lateinit var locationDataProvider: LocationDataProvider
    private val adapter = GroupAdapter<ViewHolder>().apply {
        setOnItemClickListener(this@CityPickerActivity)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_city_picker)
        citiesRecyclerView.adapter = adapter
        citiesRecyclerView.layoutManager = LinearLayoutManager(this)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setTitle(R.string.change_location)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onResume() {
        super.onResume()

        disposable += citiesViewModel.getCitiesAndSelectedCity().compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = { e ->
                            Log.e(TAG, "Error getting city list", e)
                        },
                        onNext = { (cities, selectedCityName) ->
                            Log.d(TAG, "Success, fetched ${cities.size} cities to populate toolbar")
                            adapter.clear()
                            val labels = cities.map {
                                CityItem(it, selected = it.name == selectedCityName)
                            }.toMutableList<Item<*>>().apply {
                                // No city (current location)
                                add(0, NearbyCityItem(selected = selectedCityName == null))
                            }
                            adapter.addAll(labels)
                        }
                )
    }

    override fun onItemClick(item: Item<*>, view: View) {
        if (item is BaseCityItem) {
            citiesViewModel.saveCity(item.city)
        }
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }

    override fun performInjection() {
        activityComponent.inject(this)
    }
}