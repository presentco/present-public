import React, { Component} from 'react';
import {connect} from 'react-redux';
import {bindActionCreators} from 'redux';
import * as groupActions from '../../../../../../redux/actions/groupActions';
import {withGoogleMap, GoogleMap, Marker } from "react-google-maps";
import './style.css';
import _ from 'lodash';

class MapComponent extends Component {

  constructor(props){
    super(props);
    this.handleMapLoad = this.handleMapLoad.bind(this);
  }

  handleMapLoad(map) {
      this._mapComponent = map;
      if (map) {
        console.log(map.getZoom());
      }
    }
  render() {
    let markers=[];

    if(!_.isEmpty(this.props.currentGroup)){
      markers= [{
       position: {
         lat: this.props.currentGroup.location.latitude,
         lng: this.props.currentGroup.location.longitude,
       }
     }];
   };

    //SHOWIG THE GOOGLE API MAP FOR CURRENT LOCATION
    const GettingStartedGoogleMap = withGoogleMap(props => (
      <GoogleMap
        ref={this.props.onMapLoad}
        defaultZoom={15}
        defaultCenter={{ lat: this.props.currentGroup.location.latitude, lng: this.props.currentGroup.location.longitude }}>
        {markers.map((marker,index) => (
          <Marker
            {...marker}
            key={index}
            icon={require('../../../../../../assets/images/pin.svg')}
          />
        ))}
      </GoogleMap>
    ));

    let mapClass,elementClass;

    if(this.props.currentGroup.cover){
      mapClass = "map-container-g";
      elementClass= "map-element-g";
      if(this.props.isModal){
        mapClass = "map-container-modal";
        elementClass= "map-element-modal";
      }
    } else if (!this.props.currentGroup.cover){
      mapClass="map-container-full";
      elementClass="map-element-full";
      if(this.props.isModal){
        mapClass = "map-container-modal";
        elementClass="map-element-modal";
      }

    }


    return (
      <div className="row">
        <GettingStartedGoogleMap
           containerElement={
             <div className={mapClass}/>
           }
           mapElement={
             <div className={elementClass} />
           }
           markers={markers}
           onMapLoad={this.handleMapLoad}
         />
      </div>

      )
    }
}



function mapStateToProps(state, ownProps) {
    return {
      currentGroup: state.group.currentGroup
    }
}

function mapDispatchToProps(dispatch) {
    return {
      actions: bindActionCreators({groupActions}, dispatch)
    }
}


export default connect(mapStateToProps, mapDispatchToProps)(MapComponent);
