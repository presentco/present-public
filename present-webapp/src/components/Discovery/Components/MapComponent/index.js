import React, { Component,PropTypes} from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import { withGoogleMap, GoogleMap, Marker,InfoWindow } from "react-google-maps";
import * as groupsActions from '../../../../redux/actions/groupsActions';
import groupUtiles from '../../../../core/groupUtiles';
import './style.css';
import _ from 'lodash';

class MapComponent extends Component {

  constructor(props){
    super(props);
    this.state={
      nearByGroups:props.nearByGroups ? props.nearByGroups : [],
      showInfo:[],
      filter: props.filter,
      result: props.result,
      center: props.location,
    }
  }

  componentWillReceiveProps(nextProps){
    if(this.props.filter !== nextProps.filter){
      this.updateMarkers(nextProps.filter);
    }

    if(this.props.result !== nextProps.result){
      this.updateMarkers(nextProps.result);
    }

    if (this.props.nearByGroups !== nextProps.nearByGroups) {
      this.updateMarkers({}, nextProps.nearByGroups);
    }
  }

  componentDidMount(){
    this.updateMarkers({});
  }

  updateMarkers(filter, newCircles){

    let markers=[],
      target=[],
      newTarget = this.props.nearByGroups;

      if(typeof filter === "string"){
        newTarget = filter !== "all" ? this.isCircleWomanOwned(filter , groupUtiles.filterGroups(filter, this.props.nearByGroups)) : this.props.nearByGroups;
       } else if(Array.isArray(filter)){
        newTarget = filter;
      } else if(typeof filter === 'object'){
        newTarget = newCircles ? newCircles : this.props.nearByGroups;
      }

    if(newTarget.length > 0){
      target = newTarget;
      target.map(group => {
        markers.push({
           position: {
             lat: group.location.latitude,
             lng: group.location.longitude,
           },
           title:group.title,
           location:group.locationName,
           showInfo: true,
           infoId: group.uuid,
           memberCount : group.memberCount,
           ownerPhoto:group.owner.photo,
           url: groupUtiles.getGroupUrl(group)
         });
         return markers
      })
   };

   this.setState({markers});
  }

  isCircleWomanOwned(filter, circles){
    for (let circle of circles) {
      circle.categories.map(category => {
        if (category === 'Woman Owned' && filter !== 'Woman Owned'){
          delete circles[circles.indexOf(circle)];
        }
      });
    }
    return circles;
  }

  onClickRedo(map){
    let center = {
      lat:map.state.map.center.lat(),
      lng: map.state.map.center.lng()
    };
    this.props.getCenter(center);
    this.setState({center});
  }

  showGroupInfo(infoId){
    let showInfo = this.state.showInfo;
     showInfo = _.mapValues(showInfo, () => false);
     showInfo[infoId] = !showInfo[infoId];
    this.setState({showInfo});
  }

  goToCircle(circle){
    this.context.router.push(circle);
  }

  render() {

    //SHOWIG THE GOOGLE API MAP FOR CURRENT LOCATION
    const GettingStartedGoogleMap = withGoogleMap(props => (
      <GoogleMap
        ref={props.onMapMounted}
        defaultZoom={13}
        defaultRadius={this.props.radius}
        defaultCenter={this.props.location}>
        <button className="redo-bth" onClick={this.onClickRedo.bind(this, this.refs.map)}>Redo Search here</button>
        {this.state.markers.map((marker,index) => (
          <Marker
            {...marker}
            key={index}
            icon={require('../../../../assets/images/pin.svg')}
            onClick={this.showGroupInfo.bind(this,marker.infoId)}>
          {this.state.showInfo[marker.infoId] && (
            <InfoWindow >
              <div className="info-container map-component" onClick={this.goToCircle.bind(this,marker.url)}>
                <div>
                  <p className="marker-title">
                    {marker.title}
                  </p>
                  <div className="map-component-loc-container">
                    <img alt="" src={require('../../../../assets/images/page-1.svg')} className="loc-icon map-component"/>
                    <span className="marker-name">{marker.location}</span>
                  </div>
                </div>
                <div className="member-img-name map-component">
                  <img alt="" src={marker.ownerPhoto} className="circle-owner-photo"/>
                  {marker.memberCount > 1?
                    <div className="member-discovery-container">
                      <p className="member-number">
                        +{marker.memberCount - 1}
                      </p>
                    </div> :
                    <div/>
                  }
                </div>
              </div>
            </InfoWindow>
          )}
        </Marker>
        ))}
      </GoogleMap>
    ));

    return (
      <div className="row map-x-container">
        <GettingStartedGoogleMap
           containerElement={
             <div className="map-container-d"/>
           }
           mapElement={
             <div className="map-element" />
           }
           markers={this.state.markers}
           ref="map"
         />
      </div>
      )
    }
}

MapComponent.contextTypes = {
 router: PropTypes.object
}

function mapStateToProps(state, ownProps) {
  return {
    nearByGroups: state.groups.nearByGroups,
  };
}

function mapDispatchToProps(dispatch) {
  return {
    actions: bindActionCreators(groupsActions, dispatch)
  };
}

export default connect(mapStateToProps,mapDispatchToProps)(MapComponent);
