import React, { Component} from 'react';
import {connect} from 'react-redux';
import {bindActionCreators} from 'redux';
import './style.css';
import _ from 'lodash';
import * as groupActions from '../../../../../../redux/actions/groupActions';
import * as userActions from '../../../../../../redux/actions/userActions';
import ApiHandler from '../../../../../../core/ApiHandler';
import userUtiles from '../../../../../../core/userUtiles';
import AutoCompleteAddressInput from '../../../../../common/AutoCompleteAddressInput';
import Modal from '../../../Modal';
import PhotoCropper from '../../../../../common/PhotoCropper';
import InvitePresentMembers from '../../../../../common/InvitePresentMembers';
import moment from 'moment';
import Dropdown from 'react-dropdown';
import Dropzone from 'react-dropzone';

class EidtCircle extends Component {

  constructor(props){
    super(props);

    let interests = props.currentGroup && props.type !== "create-circle" ? props.currentGroup.categories : [],
        preApproveOptions = [
        {value: "ANYONE", label: "Anyone"},
        {value: "FRIENDS_OF_MEMBERS", label:"Friends of Members"},
        {value: "FRIENDS", label:"My Friends"} ,
        {value: "INVITE_ONLY",label: "Invite Only"}
      ],
        selectedCategory = props.selectedCategory;

    this.state={
      circle: {
        uuid: props.currentGroup && props.type !== "create-circle" ? props.currentGroup.uuid : ApiHandler.guid(),
        title: props.currentGroup && props.type !== "create-circle" ? props.currentGroup.title : "",
        locationName : props.currentGroup && props.type !== "create-circle" ? props.currentGroup.locationName : "",
        location: props.currentGroup && props.type !== "create-circle" ? props.currentGroup.location : "",
        description: props.currentGroup && props.type !== "create-circle" ? props.currentGroup.description : "",
        owner: props.currentGroup.owner
      },
      interests:{
        "Work": _.some(interests, (el) => _.includes("Work", el)) || (selectedCategory && selectedCategory === 'Work'),
        "Friends": _.some(interests, (el) => _.includes("Friends", el)) || (selectedCategory && selectedCategory === 'Friends'),
        "Exercise": _.some(interests, (el) => _.includes("Exercise", el)) || (selectedCategory && selectedCategory === 'Exercise'),
        "Eat & Drink": _.some(interests, (el) => _.includes("Eat & Drink", el)) || (selectedCategory && selectedCategory === 'Eat & Drink'),
        "Communities":  _.some(interests, (el) => _.includes("Communities", el)) || (selectedCategory && selectedCategory === 'Communities'),
        "Health":  _.some(interests, (el) => _.includes("Health", el)) || (selectedCategory && selectedCategory === 'Health'),
        "Attend":  _.some(interests, (el) => _.includes("Attend", el)) || (selectedCategory && selectedCategory === 'Events'),
        "Woman Owned":  _.some(interests, (el) => _.includes("Woman Owned", el)) || (selectedCategory && selectedCategory === 'Woman Owned'),
        "Volunteer": _.some(interests, (el) => _.includes("Volunteer", el)) || (selectedCategory && selectedCategory === 'Volunteer'),
        "Shop": _.some(interests, (el) => _.includes("Shop", el)) || (selectedCategory && selectedCategory === 'Shop'),
      },
      showInterests:false,
      cover:"",
      rows:3,
      presentMembers: [],
      customCategory: "",
      customCategories: [],
      preApproveOptions: preApproveOptions,
      discoverable: props.type === "create-circle" ? true : props.currentGroup.discoverable ,
      womenOnly: props.type !== "create-circle" && props.currentGroup.space && props.currentGroup.space.id === 'women-only' ? true : false,
      selectedPreApproved: props.type !== "create-circle" && props.currentGroup.preapprove ? this.getRequestedLabel(preApproveOptions, props.currentGroup.preapprove) : preApproveOptions[0]
    }

    this.onChangeValue = this.onChangeValue.bind(this);
    this.onClickSaveCircle = this.onClickSaveCircle.bind(this);
    this.getLocation = this.getLocation.bind(this);
    this.onClickIcon = this.onClickIcon.bind(this);
    this.onChangeCoverPhoto = this.onChangeCoverPhoto.bind(this);
    this.onChangeCustomCategory = this.onChangeCustomCategory.bind(this);
    this._onSelectPreApprove = this._onSelectPreApprove.bind(this);
  }

  componentDidMount(){
    if(this.props.type === 'edit-circle'){
        this.getCustomCategories();
    }
  }

  getRequestedLabel(preApproveOptions, preapprove){
    let newApprove = {};
    for(let option of preApproveOptions){
      if(option.value === preapprove){
        newApprove = option;
        break;
      }
    }
    return newApprove;
  }

  loadRequestedGroupFromBackend(){
    let request = ApiHandler.attachHeader({url:this.props.currentGroup.url});
    this.props.actions.getGroupInfo(request).then(response => {
      this.getCustomCategories(response);
      this.props.closeEditModal();
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  componentWillReceiveProps(nextProps){
    if (this.props.currentGroup !== nextProps.currentGroup) {
        this.setState({currentGroup: nextProps.currentGroup });
    }
  }

  getCustomCategories(){
    let interests = this.props.currentGroup.categories,
        stateInterests = this.formatInterest(this.state.interests),
        customCategories=[]

        for (let interest of interests) {
            if(stateInterests.indexOf(interest) === -1){
              customCategories.push(interest);
            }
        }
        this.setState({customCategories});
  }

  onChangeValue(e){
    e.preventDefault();
    let circle = this.state.circle,
    splitCounter = (this.state.circle.description.match(/\n/g)||[]).length;
    if(e.target.name !== "numberOrder"){
      circle[e.target.name] = e.target.value;
      if(e.target.value.length > 0){
        this.props.clickInput(false);
      }

      if( splitCounter <= 7  && splitCounter >= 2){
        this.setState({rows: 9});
      }

      this.setState({circle});
    } else {
      this.setState({numberOrder: e.target.value});
    }

  }

  formatInterest(interests){
    let formattedInterest = [];
    for (let field in interests) {
      let obj = interests[field];
      if(obj === true){
        formattedInterest.push(field);
      }
    }

    return formattedInterest;
  }


  onClickSaveCircle(){

    let circle = this.state.circle,
        interests = this.state.interests,
        customCategories = this.state.customCategories,
        AllCircleCategories = this.formatInterest(interests)

       if(this.props.type === 'edit-circle'){
          if(this.state.customCategory.length > 0){
            customCategories.push(this.state.customCategory);
          }
          if(this.state.customCategories.length > 0){
            AllCircleCategories = AllCircleCategories.concat(customCategories);
          }
        } else {
            if(this.state.customCategory.length > 0){
            AllCircleCategories = AllCircleCategories.concat(this.state.customCategory);
          }
        }

    circle.categories = AllCircleCategories;

    circle["discoverable"] = this.state.discoverable;

    if(this.state.womenOnly){
      circle["spaceId"] = "women-only";
    } else {
      circle["spaceId"] = "everyone";
    }

    this.setState({circle});
    if(this.state.cover === ""){
        this.props.saveEditedCircle(circle);
    } else {
        this.props.saveEditedCircleWithPhoto(circle, this.state.cropResult);
    }
  }

  _onSelectPreApprove(selectedPreApproved){
    this.props.clickInput(true)
    let circle = this.state.circle;
    circle.preapprove = selectedPreApproved.value;

    this.setState({circle, selectedPreApproved});
  }

  getLocation(location, locationName){
    let circle = this.state.circle,
        newLocation={
          accuracy:0,
          latitude:location.lat,
          longitude:location.lng
        };

      circle.locationName = locationName;
      circle.location = newLocation;
      this.setState({circle});
  }

  onClickIcon(interest) {
     let interests = this.state.interests,
          limitInt=[];
     for (var int in this.state.interests) {
      if(this.state.interests[int] === true){
        limitInt.push(true);
      }
     }

     //3 categories per circle max
     if(limitInt.length >= 3){
       if(interests[interest]){
         interests[interest] = false;
       }
     } else {
        interests[interest] = !interests[interest];
     }
     this.setState({interests});
 }


  renderInterestButton(interest, label) {
    if(label.toLowerCase() === "style & beauty"){
      label = "Shop";
    }

     return (
         <div
           onClick={() => this.onClickIcon(label)}
           className={this.state.interests[label] ? "cursor-poiner new-category-holder old-holder" : "cursor-poiner new-category-holder"}>
           <span className={this.state.interests[label] ? "in-title-circle" : "in-title-circle non-sel"}>{label}</span>
         </div>
     );
   }

   onChangeCoverPhoto(accepted, rejected){

     let reader = new FileReader(),
         file = accepted[0],
         self = this;

     reader.onload = function(upload) {
       self.setState({cover: upload.target.result, showUncroppedPhoto:true});
     }
     reader.readAsDataURL(file);
 }


 renderCategories(){
   let categories = <div/>;
   if(!_.isEmpty(this.props.currentGroup)){
     //check if there is categories
     if(this.props.currentGroup.categories.length !== 0){
         categories = this.props.currentGroup.categories.map((interest, index) => {
           if(interest.toLowerCase() === "style & beauty"){
             interest = "Shop";
           }
            return (
             <div className="categories-con edit-categories" key={index} onClick={() => this.setState({showInterests: !this.state.showInterests})}>
               <p className="cat-name">{interest}</p>
             </div>
           )

         });
       }
   }
   return categories;
 }

 onChangeCustomCategory(e){
   e.preventDefault();
   let category = e.target.value.replace(/\w+/g, _.capitalize);
   this.setState({customCategory:category});
 }

 getCroppedCanvas(cropResult){
   this.setState({cropResult, showUncroppedPhoto: false});
 }


 onClickRemoveCustomCategory(index){
   let customCategories = this.state.customCategories;
    customCategories.splice(index, 1);
    this.setState({customCategories});
 }

 renderAddedCustomCategories(){

       if(this.state.customCategories.length > 0){
         return this.state.customCategories.map((custom, index) => {
           if(custom.length > 0){
             return(
               <div className="custom-category-container" key={index}>
                 <p className="custom-text">{custom}</p>
                 <button className="x-btn-category" onClick={this.onClickRemoveCustomCategory.bind(this, index)}>X</button>
               </div>
             )
           }
         });
       }
 }


  render() {

    let {yourCircle} = this.props,
        showUncroppedPhoto = <span/>,
        showChangeCircleOWner = <span/>,
        that = this,
        coverPhoto = this.state.cropResult ? this.state.cropResult : !yourCircle ? require('../../../../../../assets/images/purple-camera@2x.png') : yourCircle.cover ? yourCircle.cover.content : require('../../../../../../assets/images/placeholder.png');

      if(this.state.showUncroppedPhoto){
        showUncroppedPhoto= (
          <Modal show={this.state.showUncroppedPhoto}
                 noPadding={true}
                 closeModal={() => this.setState({showUncroppedPhoto: false})}>
                 <div>
                   <PhotoCropper
                     cropPhoto={this.state.cover}
                     cancelCrop={() => this.setState({showUncroppedPhoto: false})}
                     getCroppedCanvas={this.getCroppedCanvas.bind(this)}
                     />
                 </div>
          </Modal>
        )
      }

      if(this.state.changeCircleOWnerModal){

        showChangeCircleOWner=(
          <Modal show={this.state.changeCircleOWnerModal}
                 noPadding={true}
                 width={true}
                 type={'create-circle'}
                 widthSize={'594px'}
                 closeModal={() => this.setState({changeCircleOWnerModal: false})}>
                 <InvitePresentMembers
                   yourCircle={this.props.currentGroup}
                   closeModal={() => this.setState({changeCircleOWnerModal: false})}
                   loadRequestedGroupFromBackend={() => this.loadRequestedGroupFromBackend()}/>
          </Modal>
        )
      }
        return(
          <div>
            {showChangeCircleOWner}
            {showUncroppedPhoto}
            <div>
              {this.props.errorMessage ? <p className="show-error-msg">*{this.props.errorMessage}</p> : <span/>}
              <img src={require('../../../../../../assets/images/close-gray@2x.png')}
                   className="close-btn-edit right"
                   alt="close"
                   onClick={() => this.props.closeEditModal()}/>
            </div>

            <div className="edit-photo-container">
              <p className="edit-title">{this.props.type === "create-circle" ? "Create Circle" : "Edit Circle"}</p>
              <Dropzone
                id="file-upload"
                type="file"
                name="file"
                accept="image/jpeg, image/png"
                onDrop={this.onChangeCoverPhoto}
                className="dropzone-for-new-circle" >
                 <label for="file-upload" >
                     <img src={coverPhoto ? coverPhoto : require('../../../../../../assets/images/purple-camera@2x.png')} className="edit-camera" alt="upload"/>
                 </label>
                 </Dropzone>
            </div>

            <div className="edit-container">
              <div>
                <label className="edit-label">Name Your Circle</label>
                <span className="star-required">*</span>
                <input
                  name="title"
                  value={this.state.circle.title}
                  onChange={this.onChangeValue}
                  className="edit-circle-input"
                  placeholder="Something simple & concise" />
              </div>
              <div>
                <label className="edit-label location-label" onClick={() => this.setState({showAutoComplete:false})}>Location</label>
                  <span className="star-required">*</span>
                  <AutoCompleteAddressInput
                    placeholder="Place or address"
                    getLocation={this.getLocation}
                    type={this.props.type}
                    value={this.state.circle.locationName}
                    clickInput={(val) => that.props.clickInput(val)}
                    />
              </div>

                <div>
                  <label className="edit-label location-name-label">Location Name</label>
                    <input
                      name="locationName"
                      value={this.state.circle.locationName}
                      onChange={this.onChangeValue}
                      className="edit-circle-input"
                      placeholder="Location Name" />
                  </div>
                    <div>
                      <label className="edit-label description-label">Description</label>
                      <textarea
                        rows={this.state.circle.description && this.state.circle.description.length > 280 ? 9 : this.state.rows}
                        name="description"
                        value={this.state.circle.description}
                        onChange={this.onChangeValue}
                        className="edit-circle-input"
                        placeholder="What's your circle about?"></textarea>
                    </div>

                    <label className="edit-label privacy-label">Privacy</label>
                    <div className="inline-block">
                      <div className={this.state.discoverable ? "flex-row-even" : "flex-row-even because-non-discoverable"} onClick={() => this.setState({discoverable : !this.state.discoverable})}>
                        <div className="check-box-container">
                          {this.state.discoverable ? <img alt="discoverable" src={require('../../../../../../assets/images/check@3x.png')} /> : <span/>}

                        </div>
                        <div className={this.state.discoverable ? "check-text-container" : "check-text-container non-label-dicoverable"}>
                          <p className="check-box-text">Discoverable</p>
                          <p className="check-box-label">{this.state.discoverable ? 'People nearby can find this circle' : 'Hidden from search.'}</p>
                        </div>
                      </div>

                      <div className={this.state.discoverable ? "flex-row-even take-margin-left" : "flex-row-even"} onClick={() => this.setState({womenOnly : !this.state.womenOnly})}>
                        <div className="check-box-container">
                          {this.state.womenOnly ? <img alt="discoverable" src={require('../../../../../../assets/images/check@3x.png')} /> : <span/>}

                        </div>
                        <div className="check-text-container">
                          <p className={this.state.discoverable ? "check-box-text take-margin-left" : "check-box-text"}>Women-Only</p>
                          <p className={this.state.discoverable ? "check-box-label take-margin-left" : "check-box-label"}>Only women can find this circle</p>
                        </div>
                      </div>
                    </div>

                    <div className="preapprove-container">
                      <label className="edit-label approve-label">Pre-Approve</label>
                        <Dropdown
                          className={this.props.type === "create-circle" ? this.props.selectedCategory ? 'create-Circle-dropdown create-from-discovery': 'create-Circle-dropdown' : 'create-Circle-dropdown edit-Circle-dropdown'}
                          onChange={this._onSelectPreApprove}
                          value={this.state.selectedPreApproved}
                          options={this.state.preApproveOptions}/>

                    </div>


                {userUtiles.isUserAdmin(this.props.currentUser) ?
                  <div className={this.props.type === "create-circle" ? "" : "owner-circle-container"}>
                    <label className={this.props.type !== "create-circle" ? "edit-label owner-label edit-owner-label" : "edit-label owner-label"}>
                      Owner
                    </label>
                    <div className="inline-block">
                      <div className="flex-row-even">
                        {this.props.type !== "create-circle" ?
                            <img src={this.state.circle.owner.photo}
                              className="member-img" alt={this.state.circle.owner.firstName}/>
                            :
                            <img src={this.props.currentUser.photo}
                              className="member-img" alt={this.props.currentUser.name.first}/>
                        }
                        <p className="owner-name">
                          {this.props.type !== "create-circle" ? this.state.circle.owner.name : `${this.props.currentUser.name.first} ${this.props.currentUser.name.last}` }
                        </p>
                        <button onClick={() => this.setState({changeCircleOWnerModal: true})}
                          className={this.props.type === "create-circle" ? "cursor-poiner owner-change-btn" : "cursor-poiner owner-change-btn edit-owner-change-btn"}>Change</button>
                      </div>
                    </div>

                  </div>
                :
                  <span/>
                }

                 <div className={this.props.type === 'create-circle' ? "position-relative" : "position-relative category-choose-section"}>
                   <label className="edit-label category-label"
                     onClick={() => this.setState({showInterests: !this.state.showInterests, disabled: true})}>
                     Category
                   </label>
                   <label className="choose-label">Choose Up to 3</label>

                     <div className="inline-block set-margin">
                         <div>
                           <div>
                             {this.renderInterestButton('work', 'Work')}
                             {this.renderInterestButton('communities', 'Communities')}
                             {this.renderInterestButton('attend', 'Attend')}
                             {this.renderInterestButton('friends', 'Friends')}
                           </div>
                          <div>
                             {this.renderInterestButton('exercise', 'Exercise')}
                             {this.renderInterestButton('eat&drink', 'Eat & Drink')}
                              {this.renderInterestButton('health', 'Health')}
                          </div>
                          <div>
                            {this.renderInterestButton('shop', 'Shop')}
                            {this.renderInterestButton('volunteer', 'Volunteer')}
                            {this.renderInterestButton('woman', 'Woman Owned')}
                          </div>
                         </div>
                     </div>
                 </div>
                 <div>

                   {userUtiles.isUserAdmin(this.props.currentUser) ?
                     <div className="custom-container">
                       <label className="edit-label custom-label">Custom Category</label>
                       <input
                         name="custom"
                         placeholder="Add custom category"
                         value={this.state.customeCategory}
                         onChange={this.onChangeCustomCategory}
                         className="edit-circle-input"/>
                     </div>
                     : <span/>}
                     {userUtiles.isUserAdmin(this.props.currentUser) && this.props.type === 'edit-circle' ?
                     this.renderAddedCustomCategories() : <span/>}
                 </div>

            </div>
            <div className="text-center save-circle-container">
              <button className="save-circle"
                      disabled={this.state.circle.title !== ""  && this.state.circle.location !== "" ?  false : true}
                      onClick={this.onClickSaveCircle}>
                      {this.props.type==="create-circle" ? "Create Circle" : "Save Circle"}
              </button>
            </div>

          </div>
        )
    }
}


function mapStateToProps(state, ownProps) {
    return {
      currentGroup: state.group.currentGroup,
      currentUser: state.user.currentUser
    }
}

function mapDispatchToProps(dispatch) {
    return {
      actions: bindActionCreators({...groupActions,...userActions}, dispatch)
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(EidtCircle);
