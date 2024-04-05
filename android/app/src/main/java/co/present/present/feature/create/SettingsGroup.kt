package co.present.present.feature.create

import co.present.present.PresentApplication
import co.present.present.R
import co.present.present.config.FeatureDataProvider
import co.present.present.extensions.string
import co.present.present.feature.common.item.GrayHeaderItem
import co.present.present.feature.common.item.TextItem
import co.present.present.model.Circle
import co.present.present.model.CurrentUser
import co.present.present.model.Space
import co.present.present.model.isOwner
import com.xwray.groupie.Section
import present.proto.GroupMemberPreapproval

class SettingsGroup(currentUser: CurrentUser?,
                    circle: Circle?,
                    space: Space,
                    discoverable: Boolean,
                    preapproval: GroupMemberPreapproval,
                    notificationsEnabled: Boolean? = null,
                    featureDataProvider: FeatureDataProvider,
                    onSwitchChangedListener: SwitchItem.OnSwitchChangedListener,
                    onSpaceChangedListener: SwitchItem.OnSwitchChangedListener?)
    : Section() {

    constructor(currentUser: CurrentUser?,
                circle: Circle,
                featureDataProvider: FeatureDataProvider,
                onSwitchChangedListener: SwitchItem.OnSwitchChangedListener)
            : this(currentUser, circle, circle.getSpace(), circle.discoverable,
            circle.getGroupMemberPreapproval(),
            !circle.muted,
            featureDataProvider,
            onSwitchChangedListener,
            onSpaceChangedListener = null)

    init {
        add(GrayHeaderItem(stringRes = R.string.settings))

        notificationsEnabled?.let {
            add(NotificationsSwitchItem(onSwitchChangedListener, it))
        }

        val discoverabilitySubtitleResId = discoverableSubtitleResId(discoverable, space == Space.WomenOnly)

        if (circle == null || currentUser.canEdit(circle)) {
            val spaceChooserState = getSpaceChooserState(featureDataProvider, currentUser, circle)
            if (spaceChooserState != SpaceChooserState.GONE) {
                add(WomenOnlySwitchItem(
                        listener = if (spaceChooserState == SpaceChooserState.ENABLED) onSpaceChangedListener else null,
                        womenOnly = space == Space.WomenOnly)
                )
            }
            add(DiscoverabilitySwitchItem(onSwitchChangedListener, discoverabilitySubtitleResId, discoverable))
            add(ActionablePreapprovalItem(preapproval, space == Space.WomenOnly))
        } else {
            // View only items, not editable
            add(GrayHeaderItem(stringRes = R.string.privacy))

            val context = PresentApplication.staticAppComponent.application
            val sb = StringBuilder()
            with(context) {
                if (circle.isWomenOnly()) sb.append(string(R.string.women_only_sentence)).append(" ")
                sb.append(string(discoverabilitySubtitleResId)).append(" ")
                sb.append(string(circle.preApprovalSubtitleResId()))
            }

            add(TextItem(sb.toString()))
        }
    }

    private fun discoverableSubtitleResId(discoverable: Boolean, womenOnly: Boolean): Int {
        return if (discoverable) {
            if (womenOnly) R.string.discoverable_women_only_sentence else R.string.discoverable_sentence
        } else {
            R.string.non_discoverable_sentence
        }
    }

    private fun CurrentUser?.canEdit(circle: Circle?): Boolean {
        // You can't change anything about a circle while not logged in (view only)
        if (this == null) return false

        // If the circle is null, the current user is creating a totally new one, so yes you can change it
        if (circle == null) return true

        // If the circle exists, then only owners or admins can change it.
        else return isAdmin || isOwner(circle)
    }

    private fun getSpaceChooserState(featureDataProvider: FeatureDataProvider, currentUser: CurrentUser?, circle: Circle?): SettingsGroup.SpaceChooserState {
        return if (!featureDataProvider.canViewWomenOnly(currentUser)) {
            SettingsGroup.SpaceChooserState.GONE
        } else if (currentUser.canEditSpace(circle)) {
            SettingsGroup.SpaceChooserState.ENABLED
        } else {
            SettingsGroup.SpaceChooserState.DISABLED
        }
    }

    /**
     * You can only edit a circle's space if you are logged in.
     * But even if you are a woman, you can't change the space of an existing circle.
     */
    private fun CurrentUser?.canEditSpace(circle: Circle?): Boolean {
        return this != null && circle == null
    }

    enum class SpaceChooserState {
        ENABLED,
        DISABLED,
        GONE
    }
}