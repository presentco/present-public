import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import * as userActions from '../../redux/actions/userActions';
import * as groupsActions from '../../redux/actions/groupsActions';
import * as groupActions from '../../redux/actions/groupActions';
import * as commentActions from '../../redux/actions/commentActions';
import ApiHandler from '../../core/ApiHandler';
import MapComponent from './Components/MapComponent';
import Navbar from '../common/Navbar';
import GetTheApp from '../common/GetTheApp';
import SavedCircles from '../common/SavedCircles';
import _ from 'lodash';
import './style.css';
import Cards from './Components/Cards';
import groupUtiles from '../../core/groupUtiles';
import EditCircle from '../CircleHome/components/chatComponent/Components/EditCircle';
import InviteFriends from '../CircleHome/components/GeneralComponent/Components/inviteFriends';
import Spinner from 'halogen/GridLoader';
import Modal from '../CircleHome/components/Modal';
import AutoCompleteAddressInput from '../common/AutoCompleteAddressInput';
import Dropdown from 'react-dropdown';
import ExploreView from './Components/ExploreView.js'
/*global google*/

export class Discovery extends Component {
  constructor(props) {
    super(props);

    this.state = {
      circle:{},
      nearByGroups:props.nearByGroups ? props.nearByGroups : [],
      currentUser: props.currentUser ? props.currentUser : {},
      filter: this.props.location.query.interest ? this.props.location.query.interest : "All",
      showNotifications:false,
      result:[],
      searchResult: false,
      selectedCity: "",
      selectedSpace:"",
      cities:{},
      options:[],
      spaces:[],
      nearByChecked:false,
      createCircle:false,
      location: {},
      categories:[],
      matchedCities:[],
      city:"",
      searchCity:""
    };

    this.onSendQuery = this.onSendQuery.bind(this);
    this.getEventsNearBy = this.getEventsNearBy.bind(this);
    this._onSelect = this._onSelect.bind(this);
    this.onChangeValue = this.onChangeValue.bind(this);
    this.getLocation = this.getLocation.bind(this);
    this._onSelectspace = this._onSelectspace.bind(this);
    this.onClickReload = this.onClickReload.bind(this);
    this.onClickSaveCircle = this.onClickSaveCircle.bind(this);
    this.saveEditedCircleWithPhoto = this.saveEditedCircleWithPhoto.bind(this);
    this.onClickCloseShare = this.onClickCloseShare.bind(this);
  }

  componentDidMount(){
    this.getCities();
    if(this.props.category){
      this.setState({filter:this.props.category})
    }
  }

  componentWillReceiveProps(nextProps){

    if (this.props.currentUser !== nextProps.currentUser) {
      this.setState({
        selectedCity: groupUtiles.getSelectedCity(nextProps.currentUser.home).name,
        location: {
          lat:groupUtiles.getSelectedCity(nextProps.currentUser.home).location.latitude,
          lng:groupUtiles.getSelectedCity(nextProps.currentUser.home).location.longitude
        },
        currentUser: nextProps.currentUser
      });
        this.getSpaces(nextProps.currentUser);
    }

    if (this.props.nearByGroups !== nextProps.nearByGroups) {
      this.setState({nearByGroups: nextProps.nearByGroups});
      this.getAllCategories(nextProps.nearByGroups);
    }
  }

  onClickCloseShare(){
    this.setState({memberModalVisible:false});
    this.context.router.push(groupUtiles.getGroupUrl(this.state.circle));
  }


  getLocation(location, selectedCity){
     let newLocation={
              accuracy:0,
              latitude:location.lat,
              longitude:location.lng
      },
      city = {
        location:newLocation,
        name:selectedCity
      }

      this.setState({location, selectedCity,searchactive:false});
      localStorage.setItem('selectedCity', JSON.stringify(city));
      this.getEventsNearBy();
  }

  getAllCategories(nearByGroups){
    let categories = this.state.categories,
        newCategories = ["New"];

    for (var group of nearByGroups) {
      for (var category of group.categories) {
        if(categories.indexOf(category) === -1 && category !== ""){
          categories.push(category);
        }
      }
    }

    categories = _.sortBy(categories);
    if(categories.indexOf("New") !== -1){
      for (let i = 0; i < categories.length; i++) {
        if(categories[i] === 'New'){
          categories.splice(i, 1);
          newCategories = newCategories.concat(categories);
        }
      }

      this.setState({categories:newCategories});
    } else{
      this.setState({categories});
    }

  }

  getEventsNearBy(){
    let location={}

    try {
        const jsonCurrentLocation = localStorage.getItem('selectedCity');
        if (jsonCurrentLocation) {
            const currentLocation = JSON.parse(jsonCurrentLocation)
            if (currentLocation) {
                location=currentLocation.location;
            } else {
              location = this.state.currentUser.home.location;
            }
        }
    } catch (e) {
        // localStorage.removeItem('currentUser');
    }

    let request = ApiHandler.attachHeader({
      location: location
    });

    this.props.actions.getEventsNearBy(request).then(response => {
      if(response.length === 0){
        this.setState({noEvents:true});
        this.context.router.push('/app');
      } else {
        this.setState({nearByGroups: groupUtiles.getEventsBasedOnSpace(response)});
      }
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  onChangeValue(e){
    let matchedCities = [];
    for(let city of this.state.options){
      if(city.toLowerCase().indexOf(e.target.value.toLowerCase()) > -1){
        matchedCities.push(city);
      }
    }

    this.setState({city: e.target.value, matchedCities});
  }

  getCities(){
    let request = ApiHandler.attachHeader({}),
        options = [];

    this.props.actions.getCities(request).then(cities =>{
      for(let city of cities){
        options.push(city.name);
      }
      this.setState({cities, options})
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  getSelectedSpace(){
    let newSelectedSpace = "";

    try {
        const jsonCurrentSpace = localStorage.getItem('selectedSpace');

        if (jsonCurrentSpace !== undefined) {
            const selectedSpace = JSON.parse(jsonCurrentSpace)
            if (selectedSpace) {
                newSelectedSpace = selectedSpace.name
            }
        }
    } catch (e) {
        // localStorage.removeItem('currentUser');
    }
    return newSelectedSpace;
  }

  getSpaces(currentUser){
    let spaces = ["Everyone"],
        allSpaces = [{id: "everyone", name:"Everyone"}];

    if(currentUser.gender === 'WOMAN'){
      spaces.push("Women Only");
      allSpaces.push({id: "women-only", name: "Women Only"});

    }

      this.setState({spaces,allSpaces,
                    selectedSpace: this.getSelectedSpace().length === 0 ? spaces[0] : this.getSelectedSpace()});

      if(!localStorage.getItem('selectedSpace')) {
        localStorage.setItem('selectedSpace', JSON.stringify(allSpaces[0]));
      }
  }

  _onSelect (option, search) {

   let location = {lat:"", lng:""},
        cityName = typeof option === 'object' ? option.label : option;

   for(let city of this.state.cities){
     if(city.name === cityName){
       localStorage.setItem('selectedCity', JSON.stringify(city));
       this.getEventsNearBy();
       location.lat = city.location.latitude;
       location.lng = city.location.longitude;
       this.setState({selectedCity: cityName, location});

     }
   }
   if(search !== undefined){
     this.setState({searchactive:false});
   }
  }

  _onSelectspace(selectedSpace){

    this.setState({selectedSpace:selectedSpace.value});
    for(let space of this.state.allSpaces){
      if(space.name === selectedSpace.value){
        localStorage.setItem('selectedSpace', JSON.stringify(space));
      }
    }

    this.getEventsNearBy();
  }

  // This fn returns a fn so it can be used directly in JSX. This is a curried function so we
  // can pass in `filter` as context for each use.
  onClickFilter(filter) {

    if(filter === 'All'){
      this.context.router.push('/app');
    } else {
      window.history.pushState('','',`/t/${filter}`);
    }

    this.setState({filter});
  }

  renderFilterButton(filter) {
    let customName = filter === "Nearby" ?  'Trending' : filter;
    return (
      <button onClick={this.onClickFilter.bind(this,filter)}
        className={filter === "All" ? "interest-btn all-exp" : "interest-btn"}>
        <span className={this.state.filter === filter ? "active" : ""}>
          {customName}
        </span>
      </button>
    );
  }

  onClickReload(){
    this.context.router.push('/app');
    window.location.reload();
  }

  possiblyRenderNearbyGroups() {

    let showFilteredGroups = (
          <div className="text-center">
            <p className="sorry-circles">Sorry we couldnt find any circles</p>
          </div>
          );

      if(!navigator.onLine){
        window.location.reload();
      }
      // Early return is much easier to follow
      if (_.isEmpty(this.state.nearByGroups)) {
         if(!this.state.noEvents){
          return (
            <div className="margin-top-8">
              <Spinner color="#8136ec" size="20px"/>
            </div>
          );
        } else {
          return (
            <Modal show={this.state.noEvents}
                   noPadding={true}
                   closeModal={() => this.setState({noEvents: false})}>
                   <div className="text-center modal-reload">
                     <p className="reload-text">There is no circle available in this area, please try again later!</p>
                     <button className="reload-btn" onClick={this.onClickReload}>Reload</button>
                   </div>
            </Modal>
          )
       }
    }

      let filteredGroups = groupUtiles.filterGroups(this.state.filter, this.state.result.length !== 0 ?  this.state.result : this.state.nearByGroups);
      if(this.state.filter !== "Nearby") {

        // Might be worth caching these filtered groups in a Hash Map so switching filters isn't as
        // expensive if you have already done the filtering
        showFilteredGroups = (
          <Cards
            loadRequestedGroupFromBackend={(uuid) => this.loadRequestedGroupFromBackend(uuid)}
            savedGroups={this.props.savedGroups}
            currentGroup={this.props.currentGroup}
            filter={this.state.filter}
            nearByGroups={filteredGroups}
            searchResult={this.state.searchResult}/>);
      } else {
        if(this.state.searchResult){
          if(this.state.result.length !== 0){
            showFilteredGroups = (
              <Cards
                savedGroups={this.props.savedGroups}
                currentGroup={this.props.currentGroup}
                filter={this.state.filter}
                nearByGroups={this.state.result}
                searchResult={this.state.searchResult} />);
          }
        } else {
          showFilteredGroups = (
            <Cards
              savedGroups={this.props.savedGroups}
              currentGroup={this.props.currentGroup}
              filter={this.state.filter}
              nearByGroups={this.state.nearByGroups}
              searchResult={this.state.searchResult} />);
        }
      }

      return (
        <div>
          {showFilteredGroups}
        </div>
      );
  }

  onSendQuery(result){
    this.setState({result, filter: 'Nearby'});
  }

  onClickViewAll(filter){
    this.setState({filter});
    window.history.pushState('','',`/t/${filter}`)
  }

  onClickCreateCircle(category){
    this.setState({createCircle: true, selectedCategory: category });
  }

  onClickSaveCircle(updatedCircle){
    this.setState({circleLoading: true});
    let newCircle = updatedCircle;
    newCircle.center = updatedCircle.location;
    newCircle.createdFrom = updatedCircle.location;
    newCircle.radius = 0;
    let that = this;

    let request = ApiHandler.attachHeader(newCircle);

    this.props.actions.updateGroupInfo(request).then(response => {
      //that.loadUserSavedGroups();
      this.loadRequestedGroupFromBackend(response.uuid);
      that.loadGroupComments(response.uuid);
      that.setState({circleLoading:false, createCircle:false, circle:response, showAddMembers:true});
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
          console.log(errorMessage);
    });
  }

  loadRequestedGroupFromBackend(uuid){
    let request = ApiHandler.attachHeader({groupId: uuid});
     this.props.actions.loadRequestedGroup(request).then(response => {

     }).catch(error => {
       const errorMessage = ApiHandler.getErrorMessage(error);
           console.log(errorMessage);
     });

  }

  loadGroupComments(groupId){

    let request = ApiHandler.attachHeader({groupId});
    this.props.actions.loadRequestedGroupChats(request).then( response => {
      if(response){
         this.setState({comments: response});
      }
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }


  saveEditedCircleWithPhoto(updatedCircle, cover){
    this.setState({circleLoading: true})
    let newCircle = updatedCircle;
    newCircle.center = updatedCircle.location;
    newCircle.createdFrom = updatedCircle.location;
    newCircle.radius = 0;

    let requestContent = ApiHandler.attachHeader({
        uuid: ApiHandler.guid(),
        type: 'JPEG',
        content:cover.split(',')[1]
      });

    this.props.actions.uploadNewfile(requestContent).then(response => {
      newCircle.cover = {
        uuid: response.uuid,
        type: response.contentType
      };

        let request = ApiHandler.attachHeader(newCircle);
        this.props.actions.updateGroupInfo(request).then(response => {
        //  this.loadUserSavedGroups();
          this.loadGroupComments(response.uuid);
          this.loadRequestedGroupFromBackend(response.uuid);
          this.setState({circleLoading:false, createCircle:false,showAddMembers:true, circle:response})
          this.context.router.push(groupUtiles.getGroupUrl(response));
        }).catch(error => {
          const errorMessage = ApiHandler.getErrorMessage(error);
              console.log(errorMessage);
        });
    }).catch(error => {
         const errorMessage = ApiHandler.getErrorMessage(error);
         console.log(errorMessage);
      });
  }


  renderMap(){
    return (
      <MapComponent getCenter={this.getEventsNearBy}
        filter={this.state.filter}
        result={this.state.result}
        location={this.state.location}
        radius={groupUtiles.getSelectedCity(this.props.currentUser.home).radius}
        nearByGroups={this.state.nearByGroups}/>
    )
  }

  renderAllCategories(){

    return this.state.categories.map((category,index) => {
      return(
        <div className="render-all-btn" key={index}>
          { this.renderFilterButton(category)}
        </div>
      )
    });
  }

  renderCreateCircleModal(){
    return(
      <Modal show={this.state.createCircle}
             noPadding={true}
             closeModal={() => this.setState({createCircle: false})}
             width={'true'}
             widthSize={'594px'}
             height={!this.state.addHeight ? '300px' : ''}
             type={!this.state.addHeight ? '' : "create-circle"}
             clickInput={this.state.clickInput}>
             <div>
               {this.state.circleLoading ?
               <div className="spinner-container">
                 <Spinner color="#8136ec" size="150px" margin="100px"/>
               </div>
              :
               <EditCircle
                 selectedCategory={this.state.selectedCategory}
                 type="create-circle"
                 closeEditModal={() => !this.state.notSelected ? this.setState({createCircle: false}) : console.log("not selected")}
                 saveEditedCircle={this.onClickSaveCircle}
                 saveEditedCircleWithPhoto={this.saveEditedCircleWithPhoto}
                 clickInput={(val) => this.setState({clickInput: val})}/>
             }
             </div>
      </Modal>
    )
  }

  AddMembersModal(){
    return(
        <InviteFriends
          onClickClose={this.onClickCloseShare}
          currentUser={this.props.currentUser}
          memberModalVisible={true}
          groupId={this.state.circle.uuid}
          members={[]}
          fromCreateCircle={true}/>
      )
  }

  render() {

    return (
      <div>
        <Navbar onSendQuery={this.onSendQuery}
          nearByGroups={this.state.nearByGroups}
          searchVisible={true}
          userId={this.props.userId}
          location={this.state.location}
          createCircle={this.state.createCircle}
          searchResult={(value) => this.setState({searchResult: value})}/>
      <div>

      {this.state.createCircle ? this.renderCreateCircleModal() : <span/>}
      {this.state.showAddMembers ? this.AddMembersModal() : <span/>}

          <div className="small-5 medium-3 medium-uncentered large-2 no-padding large-uncentered columns">
            <GetTheApp currentUser={this.props.currentUser}/>
              <div className="city-container">
                <Dropdown className='myClassName'
                  onChange={this._onSelectspace}
                  value={this.state.selectedSpace}
                  options={this.state.spaces}/>
              </div>
              {!this.state.searchactive ?
              <div className="city-container">
                <Dropdown className='myClassName'
                  onChange={this._onSelect}
                  value={this.state.selectedCity}
                  options={this.state.options}/>
                <img src={require('../../assets/images/search.svg')}
                  className="search-icon-city cursor-pointer"
                  alt="search"
                  onClick={() => this.setState({searchactive: true})} />
              </div>
              :
              <div className="city-container">
                <AutoCompleteAddressInput
                  placeholder="Search your City"
                  getLocation={this.getLocation}
                  type={'discovery'}
                  value={this.state.searchCity}
                  location={new google.maps.LatLng(this.state.location.latitude, this.state.location.longitude)}
                  />
                <img src={require('../../assets/images/x-btn.png')}
                  className="search-icon-city close-search cursor-pointer"
                  alt="search"
                  onClick={() => this.setState({searchactive: false})} />
              </div>
            }
            {this.state.city.length > 0 && !_.isEmpty(this.state.matchedCities) && this.state.searchactive ?
              this.state.matchedCities.map((city, index) => {
                return(
                  <p key={index} className="city-search-container cursor-pointer" onClick={this._onSelect.bind(this, city, 'search')}>{city}</p>
                )
              })
            :
            <span/>}
            <SavedCircles />

          </div>
          <div className="small-7 medium-9 medium-uncentered large-10 gray-bg large-uncentered columns no-padding scrollable">

          <div>
            {!_.isEmpty(this.props.currentUser) ? this.renderMap() : <span/>}
            <div className="all-btn">
              <div>
                <button className="interest-btn all-exp refresh-btn" onClick={() => this.getEventsNearBy()}>
                  <img alt="refresh" src={require('../../assets/images/refresh@3x.png')} className="refresh-icon"/>
                </button>
                {this.renderFilterButton('All')}

                <div className="all-interests">
                  <div>
                    {this.renderFilterButton('Nearby')}
                    { this.state.categories.length > 0 ?
                      this.renderAllCategories() :
                      <span/>
                    }
                  </div>
                </div>
              </div>
            </div>
          </div>

           {this.state.filter === 'All' ?
             <ExploreView
               loadRequestedGroupFromBackend={(uuid) => this.loadRequestedGroupFromBackend(uuid)}
               nearByGroups={this.state.nearByGroups}
               onClickViewAll={(filter) => this.onClickViewAll(filter)}
               savedGroups={this.props.savedGroups}
               currentGroup={this.props.currentGroup}
               searchResult={this.props.searchResult}
               onClickCreateCircle={(title) => this.onClickCreateCircle(title)}
               />
             :
             this.possiblyRenderNearbyGroups()
           }
          </div>
        </div>
      </div>
    );
  }
}

Discovery.contextTypes = {
  router: PropTypes.object
};

function mapStateToProps(state, ownProps) {
  let userId = ownProps.params.userId,
      category = ownProps.params.categoryName;
  return {
    userId:userId,
    category:category,
    currentUser: state.user.currentUser,
    nearByGroups: state.groups.nearByGroups,
    socketUrl: state.group.socketUrl
  };
}

function mapDispatchToProps(dispatch) {
  return {
    actions: bindActionCreators({ ...userActions,...groupsActions, ...groupActions, ...commentActions}, dispatch)
  };
}

export default connect(mapStateToProps,mapDispatchToProps)(Discovery);
