
import React, {Component} from 'react';
import _ from 'lodash';
import CircleCard from '../CircleCard';
import CreateCircleCard from '../CreateCircleCard';

export class Cards extends Component {

  constructor(props){
    super(props);

    this.state={
      nearByGroups:props.nearByGroups,
      filter:props.filter,
      currentUser:{}
    }
  }

  isCircleWomanOwned(circle){
    let that = this,
        className;
    circle.categories.map(category => {
      if (category === 'Woman Owned' && that.props.filter !== 'Woman Owned' && !this.props.searchResult){
        if(this.props.type !== 'explore-view'){
          className='none';
        }
      }
    });

    return className;
  }

  renderCardsForDiscovery(){
    let showCards = <p>Sorry we couldnt find any circles available</p>;

    if(!_.isEmpty(this.props.nearByGroups)){
       showCards=this.props.nearByGroups.map(circle => {
        return(
          <div key={circle.uuid} style={{display: this.isCircleWomanOwned(circle)}} className={this.props.type === 'explore-view' ? "card-view" : "small-12 medium-6 large-3 end no-margin-bottom-circlecard columns" }>
            <CircleCard circle={circle} type={this.props.type} loadRequestedGroupFromBackend={(uuid) => this.props.loadRequestedGroupFromBackend(uuid)}/>
          </div>
        )
      })
    }
    return showCards;
  }

  renderCardsForProfile(){
    let showCards = <div className="text-center"><p>No circles!</p></div>;
    if(!_.isEmpty(this.props.nearByGroups)){
       showCards=this.props.nearByGroups.map(circle => {
        return(
          <div key={circle.uuid} className="small-12 medium-6 large-3 no-padding-left end columns">
            <CircleCard circle={circle} type={this.props.type} loadRequestedGroupFromBackend={(uuid) => this.props.loadRequestedGroupFromBackend(uuid)}/>
          </div>
        )
      })
    }

    return showCards;
  }

    render() {

      return (
        <div>
          {this.props.type !== 'user-profile' ?
            <div className="cards-container">
              {this.renderCardsForDiscovery()}
                { this.props.addPhoto !== undefined  && this.props.filter !== 'New' && this.props.filter !== 'Nearby' ?
                  <CreateCircleCard
                    title={this.props.filter}
                    addPhoto={this.props.addPhoto}
                    onClickCreateCircle={() => this.props.onClickCreateCircle(this.props.filter)}/> : <span/>
                }

            </div> :

              this.renderCardsForProfile()
            }
        </div>

      );
      }
}

export default Cards;
